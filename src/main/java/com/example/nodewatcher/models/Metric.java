package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import java.sql.Timestamp;

public abstract class Metric
{

  private final String ip;

  private final Timestamp timestamp;

  public Metric(String ip, Timestamp timestamp)
  {
    this.ip = ip;

    this.timestamp = timestamp;

  }

  public String getIp()
  {
    return ip;
  }

  public Timestamp getTimestamp()
  {
    return timestamp;
  }

  public abstract Tuple getTuple(int discoveryId);

  public static Metric fromJson(JsonObject device,Timestamp timestamp)
  {

    return device.getString("free")!=null

      ? MemoryMetric.fromJson(device, timestamp)

      : CpuMetric.fromJson(device,timestamp);

  }

}
