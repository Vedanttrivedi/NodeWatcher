package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginDataSender extends AbstractVerticle
{
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  private final AtomicBoolean startPolling;

  private final SqlClient sqlClient;

  private final ZMQ.Socket socket;

  private final JsonArray responseData;

  private final ZContext context;

  public PluginDataSender(SqlClient sqlClient, ZContext context)
  {

    this.context = context;

    this.sqlClient = sqlClient;

    this.socket = context.createSocket(SocketType.PUSH);

    this.socket.bind(Address.PUSHSOCKET);

    this.responseData = new JsonArray();

    this.startPolling = new AtomicBoolean(false);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    vertx.eventBus().<JsonObject>localConsumer(Address.PLUGINDATASENDER, pluginSenderHandler->{

      handleNewDeviceData(pluginSenderHandler.body());

    });
    startPromise.complete();

    fetchAndProcessDiscoveries();

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

        atLeastOnePingable.set(true);
      }
      else
      {
          logger.info("Bootstrap : Device is Down "+ip);

      }
      checkIfProcessingComplete(remainingRows, atLeastOnePingable);
    });

  }

  private void addDeviceToResponseData(io.vertx.sqlclient.Row row)
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

      var base64Encoded = Base64.getEncoder().encode(data.encode().getBytes());

      socket.send(base64Encoded,ZMQ.DONTWAIT);

      startPolling.set(true);

      System.out.println("Data sent successfully");

      startCpuPolling();

    }

    catch (Exception e)
    {
      System.out.println("Error happen while sending data");

    }


  }

  private JsonObject createFetchDetailsObject()
  {
    return new JsonObject()
      .put("metrics", "all")
      .put("memory", "free\tused\tswap\tdisc_used\tcache")
      .put("cpu", "percentage\tload_average\tprocess_count\tio_percent\tthreads")
      .put("device", "name\tos_name\tarchitecture\tuptime\tkernel")
      .put("iteration", 1);

  }

  private void startCpuPolling()
  {
    final long[] lastMemoryPoll = {System.currentTimeMillis()};
    final long[] lastCpuPoll = {System.currentTimeMillis()};

    vertx.setPeriodic(100, handler ->
    {
      long currentTime = System.currentTimeMillis();

      if (currentTime - lastMemoryPoll[0] >= Address.MEMORYINTERVAL)
      {

        if (startPolling.get())
        {

          var data = new JsonObject();

          data.put("metric", "memory");

          var jsonArray = new JsonArray();

          jsonArray.add(data);

          socket.send(Base64.getEncoder().encode(jsonArray.encode().getBytes()));

        }
        lastMemoryPoll[0] = currentTime; // Update last memory poll time
      }

      if (currentTime - lastCpuPoll[0] >= Address.CPUNTERVAL)
      {
        if (startPolling.get())
        {

          var data = new JsonObject();

          data.put("metric", "cpu");

          var jsonArray = new JsonArray();

          jsonArray.add(data);

          socket.send(Base64.getEncoder().encode(jsonArray.encode().getBytes()));

        }
        lastCpuPoll[0] = currentTime; // Update last CPU poll time
      }
    });

  }
}
