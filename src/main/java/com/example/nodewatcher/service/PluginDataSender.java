package com.example.nodewatcher.service;

import com.example.nodewatcher.Bootstrap;
import com.example.nodewatcher.models.Device;
import com.example.nodewatcher.utils.Address;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.Map;

public class PluginDataSender extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(PluginDataSender.class);

  private final ZMQ.Socket socket;

  private Boolean pollStarted;

  private final SqlClient sqlClient;

  private final Map<String, Device> monitoredDevice;

  public PluginDataSender()
  {

    pollStarted = false;

    socket = Bootstrap.zContext.createSocket(SocketType.PUSH);

    socket.bind(Address.PUSH_SOCKET);

    sqlClient = Bootstrap.databaseClient;

    monitoredDevice = new HashMap<>();

  }

  @Override
  public void start(Promise<Void> startPromise)
  {

     init();//send all provisioned devices to plugin if they are up

     //listen for client request to provision the device at runtime
     vertx.eventBus().<JsonArray>localConsumer(Address.PLUGIN_DATA_SENDER, handler->{

       send(handler.body());

     });

     startPromise.complete();

  }


  private void send(JsonArray data)
  {

      var status = socket.send(data.encode().getBytes(),ZMQ.DONTWAIT);

      if(!pollStarted && status)
      {
        vertx.eventBus().send("poll","Start Polling");

        pollStarted = true;

      }

  }
  private void init()
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
      ping(row);

    });

  }

  private void ping(Row row)
  {

    var discoveryAndCredential = new JsonObject()

    .put("discoveryName", row.getString(0))

    .put("ip", row.getString(1))

    .put("username", row.getString(2))

    .put("password", row.getString(3))

    .put("doPolling", true);

    vertx.executeBlocking(pingPromise ->
    {

      pingPromise.complete(Config.ping(row.getString(1)));

    },false,pingFuture ->
    {

      if ((boolean) pingFuture.result())
      {

        var temper = new JsonArray();

        temper.add(discoveryAndCredential);

        temper.add(false);

        vertx.eventBus().send(Address.PLUGIN_DATA_SENDER, temper);

      }
      else
      {
        vertx.eventBus().send(Address.UNREACHED_DISCOVERY, discoveryAndCredential);
      }
    });

  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception
  {
    socket.close();

    stopPromise.complete();

  }
}
