package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PingVertical extends AbstractVerticle
{

  @Override
  public void start()
  {

    vertx.eventBus().<JsonObject>consumer(Address.pingCheck, this::handlePingRequest);

  }

  private void handlePingRequest(Message<JsonObject> message)
  {
    var data = message.body();

    System.out.println("I received request "+data.getString("ip"));

    canPing(data.getString("ip"),22).

      onComplete(result->{

        System.out.println("Ping promise completed "+result.result());

        if(result.succeeded())
        {
          System.out.println("Device is reached");

          message.reply("Device is up");

        }
        else
          message.fail(500, result.cause().getMessage());

      });

  }

  private Future<Boolean> canPing(String ip,int port)
  {
    var promise = Promise.<Boolean>promise();

    var future = promise.future();

    System.out.println("About to ping "+ip);

    vertx.executeBlocking(

      handler->{

      var processBuilder  = new ProcessBuilder("fping","-c","3",ip);

      System.out.println("Execute blocking !");

      try
      {
        var process = processBuilder.start();

        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;

        var counts = 0;

        while((line = reader.readLine())!=null)
        {

          if(line.contains("0% loss"))
            counts++;

        }
        if(counts==3)
          promise.complete(true);

        else
          promise.fail("Device is Down!");

      }
      catch (IOException e)
      {

        promise.fail("Error in processbuilder!");

      }

    });

    return promise.future();

  }

}
