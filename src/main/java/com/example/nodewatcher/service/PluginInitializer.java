package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import  org.slf4j.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginInitializer extends AbstractVerticle
{
  private static final Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  private final SqlClient sqlClient;

  public PluginInitializer(SqlClient sqlClient)
  {

    this.sqlClient = sqlClient;

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    vertx.eventBus().<JsonObject>localConsumer(Address.PLUGIN_DATA_SENDER, pluginSenderHandler->{

      handleNewDeviceData(pluginSenderHandler.body());

    });
    startPromise.complete();

    fetchAndProcessDiscoveries();

  }

  private void handleNewDeviceData(JsonObject device)
  {
    sendDataToPlugin(device);
  }

  private void fetchAndProcessDiscoveries()
  {

    vertx.executeBlocking(promise->{

      sqlClient.query("SELECT d.name, d.ip, c.username, c.password, c.protocol " +
          "FROM Discovery d JOIN Credentials c ON d.credentialID = c.id WHERE d.is_provisioned = true")
        .execute()

        .onComplete(result -> {

          if (result.succeeded())
          {
            promise.complete(result.result());
          }
          else
          {
            promise.fail("Error Fetching Details ");
          }

        });

    },future->{
      if(future.succeeded())

        processDiscoveryResults((RowSet<Row>) future.result());
      else
        System.out.println("Cannot fetch");

    });
  }

  private void processDiscoveryResults(RowSet<Row> rows)
  {
    rows.forEach(row ->
    {
      var ip = row.getString(1);

      ping(ip, row);

    });

  }

  private void ping(String ip,Row row)
  {
    var pingBody = new JsonObject().put("ip", ip);

    vertx.eventBus().request(Address.PINGCHECK, pingBody, reply -> {

      if (reply.succeeded())
      {

        var discoveryAndCredential = new JsonObject()
          .put("discoveryName", row.getString(0))
          .put("ip", row.getString(1))
          .put("username", row.getString(2))
          .put("password", row.getString(3))
          .put("doPolling",true);

         sendDataToPlugin(discoveryAndCredential);
      }
      else
      {
          logger.info("Bootstrap : Device is Down "+ip);

      }

    });

  }

  private void sendDataToPlugin(JsonObject data)
  {
    var fetchDetails = createFetchDetailsObject();

    var tempData = new JsonArray();

    tempData.add(data);

    tempData.add(fetchDetails);

    vertx.eventBus().send("send",tempData);

  }

  private JsonObject createFetchDetailsObject()
  {
    return new JsonObject()
      .put("Sample",true);
  }
}
