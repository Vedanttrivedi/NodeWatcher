package com.example.nodewatcher.utils;

import com.jcraft.jsch.JSch;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Config
{

  public final static int HTTP_PORT = 4500;
  public final static int DATA_SAVER_INSTANCES = 5;
  public final static int DB_POOL_SIZE = 5;

  public static boolean validIp(String ip)
  {
    var regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";

    var pattern = Pattern.compile(regex);

    var matcher = pattern.matcher(ip);

    return matcher.matches();

  }

  public static boolean ping(String ip)
  {
    try
    {
      var processBuilder  = new ProcessBuilder("ping","-c 2",ip);

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

        process.destroy();

        return false;
      }

      if(counts > 0)
      {
        System.out.println("Completed");

        return true;
      }

    }
    catch (Exception exception)
    {
      return false;
    }

    return false;
  }

  public static boolean  ssh(String username,String password,String ip)
  {
    try
    {
      var jsch = new JSch();

      var session = jsch.getSession(username, ip, 22);

      session.setTimeout(2000);

      session.setPassword(password);

      session.setConfig("StrictHostKeyChecking", "no");

      session.connect();

      session.disconnect();

      return true;
    }

    catch (Exception exception)
    {
      return false;
    }
  }

}

