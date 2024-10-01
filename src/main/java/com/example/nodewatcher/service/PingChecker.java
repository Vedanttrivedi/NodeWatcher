package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import com.jcraft.jsch.JSch;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PingChecker extends AbstractVerticle
{

  @Override
  public void start()
  {

    vertx.eventBus().<JsonObject>localConsumer(Address.PINGCHECK, this::handlePingRequest);

    vertx.eventBus().<JsonObject>localConsumer(Address.SSHCHECK, this::handleSSHRequest);


  }

  private void handlePingRequest(Message<JsonObject> message)
  {
    var data = message.body();

    ping(data.getString("ip")).

      onComplete(result->{


        if(result.succeeded())
        {

          message.reply("Device is up");

        }
        else
          message.fail(500, result.cause().getMessage());

      });

  }
  private void handleSSHRequest(Message<JsonObject> message)
  {
    var data = message.body();

    var ip = data.getString("ip");

    var password = data.getString("password");

    var username = data.getString("username");

    ssh(ip,username,password).onComplete(
      authResult->{

        if(authResult.succeeded())
        {
          message.reply("Discovered");
        }
        else
          message.reply(authResult.cause());
    });

  }

  private Future<Boolean> ping(String ip)
  {
    var promise = Promise.<Boolean>promise();

    var processBuilder  = new ProcessBuilder("fping","-c","3",ip);

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

    return promise.future();

  }

  private Future<Boolean> ssh(String ip,String username,String password)
  {
    var promise = Promise.<Boolean>promise();

    try
    {
      var jsch = new JSch();

      var session = jsch.getSession(username, ip, 22);

      session.setTimeout(2000);

      session.setPassword(password);

      session.setConfig("StrictHostKeyChecking", "no");

      session.connect();

      System.out.println("Authenticaion Completed!");

      promise.complete(true);

      session.disconnect();

    }
    catch (Exception exception)
    {
        promise.fail("Authentication Error!");
    }

    return promise.future();

  }

}
