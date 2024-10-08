package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import com.jcraft.jsch.JSch;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HostReachabilityChecker extends AbstractVerticle
{
  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    vertx.eventBus().<JsonObject>localConsumer(Address.PINGCHECK, this::handlePingRequest);

    vertx.eventBus().<JsonObject>localConsumer(Address.SSHCHECK, this::handleSSHRequest);

    startPromise.complete();
  }

  private void handlePingRequest(Message<JsonObject> message)
  {

    vertx.executeBlocking(pingResultPromise->{

      var processBuilder  = new ProcessBuilder("fping","-c","3",message.body().getString("ip"));

      try
      {
        var process = processBuilder.start();

        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;

        var counts = 0;

        while((line = reader.readLine())!=null)
        {

          if(line.contains("0% loss"))
          {
            counts++;
            break;
          }

        }
        if(counts > 0)
          pingResultPromise.complete(true);

        else
          pingResultPromise.fail("Device is Down!");

      }
      catch (Exception exception)
      {

        pingResultPromise.fail("Error in processbuilder!");

      }

    },false,
    pingResultFuture->{

      if(pingResultFuture.succeeded())
      {
          message.reply("Device In Reach");
      }
      else
      {
        message.fail(1,"Device not in reach!");
      }
    });


  }
  private void handleSSHRequest(Message<JsonObject> message)
  {
    var data = message.body();

    System.out.println("data "+data );
    var ip = data.getString("ip");

    var password = data.getString("password");

    var username = data.getString("username");

    vertx.executeBlocking(sshFuture->{

      try
      {
        var jsch = new JSch();

        var session = jsch.getSession(username, ip, 22);

        session.setTimeout(2000);

        session.setPassword(password);

        session.setConfig("StrictHostKeyChecking", "no");

        session.connect();

        sshFuture.complete(true);

        session.disconnect();

      }
      catch (Exception exception)
      {
        sshFuture.fail("Authentication Error!");
      }

    },false,sshFutureRes->
    {
      if(sshFutureRes.succeeded())
        message.reply("Discovered");
      else
        message.fail(1,"Authentication ERROR");

    });

  }

}
