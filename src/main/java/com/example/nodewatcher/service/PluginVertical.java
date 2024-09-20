package com.example.nodewatcher.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.ZMQ;

import java.time.LocalDateTime;
import java.util.Base64;

public class PluginVertical extends AbstractVerticle
{
  SqlClient sqlClient;

  ZMQ.Socket socket;

  public PluginVertical(SqlClient sqlClient,ZMQ.Socket socket)
  {
    this.sqlClient = sqlClient;
    this.socket = socket;

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    //When the vertical is loaded it should send the all provisioned device with list of metric information available
    var responseData = new JsonArray();

    System.out.println("Plugin Vertical Loaded");

    sqlClient.
      query("SELECT d.name,d.ip,c.username,c.password,c.protocol " +
        "FROM Discovery d JOIN Credentials c ON d.credentialID = c.id WHERE d.is_provisioned = true")
      .execute()

      .onComplete(result -> {

        if(result.succeeded())
        {
          System.out.println("loaded plugin ");

          result.result().forEach(row->
          {
            var discoveryAndCredential = new JsonObject();

            discoveryAndCredential.put("discoveryName",row.getString(0));

            discoveryAndCredential.put("ip",row.getString(1));

            discoveryAndCredential.put("username",row.getString(2));

            discoveryAndCredential.put("password",row.getString(3));

            responseData.add(discoveryAndCredential);

          });


          var cpu_metrics = "percentage\tload_average\tprocess_count\tio_percent\tthreads";

          var memory_metrics = "free\tused\tswap\tdisc_used\tcache";

          var device_metrics = "name\tos_name\tarchitecture\tuptime\tkernel";


          //This fetch Details is a last object in what we need to send,
          //It defines what we need to collect from plugin
          //plugin will first check this to decide waht information it needs to get
          //if metric is set to all then it will check for all cpu,memory,network string.
          //then split the string to count the countables;


          var fetchDetails = new JsonObject();

          fetchDetails.put("metrics","all");

          fetchDetails.put("memory",memory_metrics);

          fetchDetails.put("cpu",cpu_metrics);

          fetchDetails.put("device",device_metrics);

          fetchDetails.put("iteration",1);

          responseData.add(fetchDetails);

          System.out.println("Response "+responseData.encodePrettily());

          var base64Encoded = Base64.getEncoder().encode(responseData.encode().getBytes());

          System.out.println("After encoding : "+base64Encoded);

          vertx.executeBlocking(promise->{
            try
            {
              socket.send(base64Encoded);

              System.out.println("Data sent! First Time");

              startPromise.complete();
            }
            catch (Exception e)
            {

              System.out.println("Error while sending data "+e.getCause());

            }

          }, future->
            {

              System.out.println("Future result "+future.result());

          });


        }
      });

  }
}
