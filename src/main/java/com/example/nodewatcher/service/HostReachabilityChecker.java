package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import com.jcraft.jsch.JSch;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

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

      var processBuilder  = new ProcessBuilder("ping","-c 2",message.body().getString("ip"));

      try
      {

        var process = processBuilder.start();

        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;

        var counts = 0;

        while((line = reader.readLine())!=null)
        {

          if(line.contains("ttl"))
          {
            counts++;

            break;
          }

        }

        var status = process.waitFor(3, TimeUnit.SECONDS);

        if(!status || counts ==0)
        {

          System.out.println("Not Completed");

          pingResultPromise.fail("Device is Down!");

          process.destroy();

        }

        if(counts > 0)
        {
          System.out.println("Completed");

          pingResultPromise.complete(true);

        }


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

    var ip = data.getString("ip");

    var password = data.getString("password");

    var username = data.getString("username");

    vertx.executeBlocking(sshFuture ->
    {
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

    }, false, sshFutureRes ->
    {
      if (sshFutureRes.succeeded())
      {

        message.reply("Discovered");
      }
      else
        message.fail(1, "Authentication ERROR");
    });

  }
}
