package com.example.nodewatcher.service;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.LoggerFactory;
import  org.slf4j.*;

public class PluginInitializer extends AbstractVerticle
{
  private static final Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  private final SqlClient sqlClient;

  public PluginInitializer()
  {

    this.sqlClient = BootStrap.getDatabaseClient();

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    vertx.eventBus().<JsonObject>localConsumer(Address.UPDATE_DISCOVERY, pluginSenderHandler->{

      System.out.println("Handler "+pluginSenderHandler.body());

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

    sqlClient.query("SELECT d.name, d.ip, c.username, c.password, c.protocol " +
        "FROM Discovery d JOIN Credentials c ON d.credentialID = c.id WHERE d.is_provisioned = true")
      .execute()

      .onComplete(result -> {

        if (result.succeeded())
        {
          processDiscoveryResults(result.result());
        }
        else
        {
          System.out.println("Error while fetching data!");
        }

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

    vertx.eventBus().request(Address.PINGCHECK, pingBody, new DeliveryOptions().setSendTimeout(3000), reply -> {

      var discoveryAndCredential = new JsonObject()
        .put("discoveryName", row.getString(0))
        .put("ip", row.getString(1))
        .put("username", row.getString(2))
        .put("password", row.getString(3));

      if(reply.succeeded())
      {
          discoveryAndCredential.put("doPolling",true);

          sendDataToPlugin(discoveryAndCredential);
      }
      else
      {

        vertx.eventBus().send(Address.UNREACHED_DISCOVERY,discoveryAndCredential);

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

    vertx.eventBus().send(Address.PLUGIN_DATA_SENDER,tempData);

  }

  private JsonObject createFetchDetailsObject()
  {
    return new JsonObject()
      .put("Sample",true);
  }
}
