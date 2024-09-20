package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record Memory_Metric(String ip, int free, int used, int swap, int cached, int disk_space, boolean status)
{
  public static Memory_Metric fromJson(JsonObject object)
  {
    try
    {
      var ip = object.getString("ip");

      var free = Integer.valueOf(object.getString("free"));

      var swap = Integer.valueOf(object.getString("swap"));

      var used = Integer.valueOf(object.getString("used"));

      var cached = Integer.valueOf(object.getString("cached"));

      var disk_space = Integer.valueOf(object.getString("disk_space"));

      System.out.println("Status failed ");
      return new Memory_Metric(ip,free,swap,used,cached,disk_space,object.getBoolean("status"));

    }
    catch (Exception e)
    {
      System.out.println("failed for "+object);
    }
    return new Memory_Metric("ip",00,0,0,0,0,false);
  }

}
