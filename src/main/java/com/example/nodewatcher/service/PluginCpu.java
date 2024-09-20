package com.example.nodewatcher.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointerIterator;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.time.LocalDateTime;
import java.util.Base64;

public class PluginCpu extends AbstractVerticle
{

  ZMQ.Socket socket;

  public PluginCpu(ZMQ.Socket socket)
  {
    this.socket = socket;

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    //When the vertical is loaded it should send the all provisioned device with list of metric information available
    System.out.println("Cpu plugin Vertical Loaded");


    vertx.setPeriodic(5000,cpuhandler->{

      System.out.println("Cpu periodic "+LocalDateTime.now().toString());
      //sent periodical request to fetch memory data
      var payload = new JsonArray();

      var metricToSend = new JsonObject();

      metricToSend.put("metric","cpu");

      payload.add(metricToSend);

      var response = Base64.getEncoder().encode(payload.encode().getBytes());

      vertx.executeBlocking(blockingHandler->{

        socket.send(response,ZMQ.DONTWAIT);

      });

    });

  }
}
