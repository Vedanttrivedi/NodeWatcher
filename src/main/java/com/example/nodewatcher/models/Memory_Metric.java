package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Memory_Metric extends Metric
{
  private int free;
  private int used;
  private int swap;
  private int cached;
  private int disk_space;
  private boolean status;


  public Memory_Metric(String ip, Timestamp timestamp, int free, int used, int swap, int cached, int disk_space, boolean status) {
    super(ip,timestamp);
    this.free = free;
    this.used = used;
    this.swap = swap;
    this.cached = cached;
    this.disk_space = disk_space;
    this.status = status;
  }
  @Override
  public JsonObject toJson()
  {
    return new JsonObject()
      .put("ip", getIp())
      .put("timestamp", getTimestamp().toString())
      .put("free", free)
      .put("used", used)
      .put("swap", swap)
      .put("cached", cached)
      .put("diskSpace", disk_space);
  }
  @Override

  public Tuple getTuple(int discoveryId)
  {
    return Tuple.of(discoveryId, free, used, swap, cached, disk_space, getTimestamp());
  }

  public static Memory_Metric fromJson(JsonObject object,Timestamp timestamp)
  {
    try
    {
      var ip = object.getString("ip");

      var free = Integer.valueOf(object.getString("free"));

      var swap = Integer.valueOf(object.getString("swap"));

      var used = Integer.valueOf(object.getString("used"));

      var cached = Integer.valueOf(object.getString("cached"));

      var disk_space = Integer.valueOf(object.getString("disk_space"));


      return new Memory_Metric(ip,timestamp,free,swap,used,cached,disk_space,object.getBoolean("status"));

    }
    catch (Exception e)
    {
      System.out.println("failed for "+object);
    }
    return new Memory_Metric("ip",Timestamp.valueOf(LocalDateTime.now()),0,0,0,0,0,false);
  }

  public int getFree()
  {
    return free;
  }

  public int getUsed() {
    return used;
  }

  public int getSwap() {
    return swap;
  }

  public int getCached() {
    return cached;
  }

  public int getDisk_space() {
    return disk_space;
  }

}

