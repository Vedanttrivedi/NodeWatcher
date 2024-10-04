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

  private final JsonArray responseData;


  public PluginInitializer(SqlClient sqlClient)
  {

    this.sqlClient = sqlClient;

    this.responseData = new JsonArray();

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    vertx.eventBus().<JsonObject>localConsumer(Address.PLUGIN_DATA_SENDER, pluginSenderHandler->{

      handleNewDeviceData(pluginSenderHandler.body());

    });

    fetchAndProcessDiscoveries();

    startPromise.complete();

  }


  private void handleNewDeviceData(JsonObject device)
  {

    var updatedRequestResponse = new JsonArray().add(device);

    sendDataToPlugin(updatedRequestResponse);

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
          logger.error("Error Fetching Details ");
        }

      });
  }

  private void processDiscoveryResults(RowSet<Row> rows)
  {
    AtomicInteger remainingRows = new AtomicInteger(rows.size());

    AtomicBoolean atLeastOnePingable = new AtomicBoolean(false);

    rows.forEach(row ->
    {
      String ip = row.getString(1);

      pingDevice(ip, row, remainingRows, atLeastOnePingable);

    });

  }

  private void pingDevice(String ip,Row row, AtomicInteger remainingRows, AtomicBoolean atLeastOnePingable)
  {
    JsonObject pingBody = new JsonObject().put("ip", ip);

    vertx.eventBus().request(Address.PINGCHECK, pingBody, reply -> {

      if (reply.succeeded())
      {
        addDeviceToResponseData(row);

        if(!atLeastOnePingable.get())

          atLeastOnePingable.set(true);
      }
      else
      {
          logger.info("Bootstrap : Device is Down "+ip);

      }
      checkIfProcessingComplete(remainingRows, atLeastOnePingable);
    });

  }

  private void addDeviceToResponseData(Row row)
  {
    var discoveryAndCredential = new JsonObject()
      .put("discoveryName", row.getString(0))
      .put("ip", row.getString(1))
      .put("username", row.getString(2))
      .put("password", row.getString(3))
      .put("doPolling",true);

    responseData.add(discoveryAndCredential);

  }

  private void checkIfProcessingComplete(AtomicInteger remainingRows, AtomicBoolean atLeastOnePingable)
  {
    if (remainingRows.decrementAndGet() == 0)
    {
      if (atLeastOnePingable.get())
      {

        sendDataToPlugin(responseData);
      }
      else
      {
          logger.error("App Start: No Ping Device Found!");

      }
    }
  }

  private void sendDataToPlugin(JsonArray data)
  {
    var fetchDetails = createFetchDetailsObject();

    data.add(fetchDetails);

    try
    {

      vertx.eventBus().send("send",data);

    }

    catch (Exception e)
    {
      System.out.println("Error happen while sending data");

    }


  }

  private JsonObject createFetchDetailsObject()
  {
    return new JsonObject()
      .put("Sample",true);
  }

}
