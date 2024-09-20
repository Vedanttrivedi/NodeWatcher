package com.example.nodewatcher.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.zeromq.ZMQ;

import java.time.LocalDateTime;
import java.util.Base64;

public class PluginMemory extends AbstractVerticle
{
  private ZMQ.Socket socket;

  public PluginMemory(ZMQ.Socket socket)
  {
    this.socket = socket;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    vertx.setPeriodic(3000,memhandler->{

      System.out.println("Memort Metric "+ LocalDateTime.now().toString());
      //sent periodical request to fetch memory data
      var payload = new JsonArray();

      var metricToSend = new JsonObject();

      metricToSend.put("metric","memory");

      payload.add(metricToSend);

      var response = Base64.getEncoder().encode(payload.encode().getBytes());

      vertx.executeBlocking(blockingHandler->{

        socket.send(response);

      });

    });


  }
}
