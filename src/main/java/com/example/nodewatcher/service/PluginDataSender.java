package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginDataSender implements Runnable
{

  private AtomicBoolean startPolling;

  private final SqlClient sqlClient;

  private final ZMQ.Socket socket;

  private final JsonArray responseData;

  private final ZContext context;

  private Vertx vertx;

  public PluginDataSender(SqlClient sqlClient, ZContext context, Vertx vertx)
  {
    this.vertx = vertx;
    this.context = context;

    this.sqlClient = sqlClient;

    this.socket = context.createSocket(SocketType.PUSH);

    this.socket.bind("tcp://localhost:4555");

    this.responseData = new JsonArray();

    startPolling = new AtomicBoolean();

    startPolling.set(false);
  }

  public void run()
  {
    var cpuPollingTime = 5000;

    var memoryPollingTime = 3000;


    System.out.println("Plugin Sender Loaded");

    try
    {
      //also listen for new updated device updated request



      //Operations to perform on sql discovery

      sqlClient.query("SELECT d.name,d.ip,c.username,c.password,c.protocol " +
          "FROM Discovery d JOIN Credentials c ON d.credentialID = c.id WHERE d.is_provisioned = true")
        .execute()

        .onComplete(result -> {

          if (result.succeeded())
          {
            AtomicInteger remainingRows = new AtomicInteger(result.result().size());

            System.out.println("Total Rows are "+remainingRows.get());

            result.result().forEach(row ->
            {
              var ip = row.getString(1);

              var pingBody = new JsonObject();

              pingBody.put("ip", ip);

              vertx.eventBus().request(Address.pingCheck, pingBody, reply ->
              {
                if (reply.succeeded())
                {
                  var discoveryAndCredential = new JsonObject();

                  discoveryAndCredential.put("discoveryName", row.getString(0));

                  discoveryAndCredential.put("ip", row.getString(1));

                  discoveryAndCredential.put("username", row.getString(2));

                  discoveryAndCredential.put("password", row.getString(3));

                  System.out.println("Adding this discovery " + discoveryAndCredential);

                  responseData.add(discoveryAndCredential);
                }
                else
                {

                  System.out.println("Device is Down " + ip);

                }

                if (remainingRows.decrementAndGet() == 0)
                {

                  System.out.println("I have processede everything about to send data through zmq "+responseData);

                  addFetchDetailsAndExecuteBlocking(responseData);

                  if(startPolling.get())
                  {
                    System.out.println("Data of devices of is sent to plugin");


                  }

                }
              });
            });
          }
          else
          {
            System.out.println("Failed To  Query Database");
          }
        });
    }
    catch (Exception exception)
    {
      System.out.println("Error in main plugin exception " + exception.getLocalizedMessage());

    }
  }

  private void addFetchDetailsAndExecuteBlocking(JsonArray responseData)
  {
    var cpu_metrics = "percentage\tload_average\tprocess_count\tio_percent\tthreads";

    var memory_metrics = "free\tused\tswap\tdisc_used\tcache";

    var device_metrics = "name\tos_name\tarchitecture\tuptime\tkernel";

    var fetchDetails = new JsonObject();

    fetchDetails.put("metrics", "all");

    fetchDetails.put("memory", memory_metrics);

    fetchDetails.put("cpu", cpu_metrics);

    fetchDetails.put("device", device_metrics);

    fetchDetails.put("iteration", 1);

    responseData.add(fetchDetails);

    vertx.executeBlocking(promise -> {
      try
      {

        System.out.println("Original Data " + responseData.encodePrettily());

        var base64Encoded = Base64.getEncoder().encode(responseData.encode().getBytes());

        System.out.println("Encoded Data to sent " + base64Encoded);

        socket.send(base64Encoded);

        startPolling.set(true);

        System.out.println("Data sent! First Time or New Devices ");

        promise.complete();

      }
      catch (Exception e)
      {
        promise.fail("Error " + e.getMessage());

        System.out.println("Error while sending data " + e.getMessage());

      }
    },
      future ->
      {
      if (future.succeeded())
      {
        System.out.println("Complete Sending");

        //startPromise.complete();
      }
      else
      {
        System.out.println("Error while sending");

        //startPromise.fail("Failed ");

      }
    });
  }
}
