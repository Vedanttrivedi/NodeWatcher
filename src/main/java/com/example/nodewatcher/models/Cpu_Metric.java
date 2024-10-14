package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

import java.sql.Timestamp;

public class Cpu_Metric extends Metric
{
  private float percentage;

  private float load_average;

  private int process_counts;

  private int threads;

  private float io_percent;

  private boolean status;


  public Cpu_Metric(String ip, Timestamp timestamp, float percentage, float load_average, int process_counts, int threads, float io_percent, boolean status)
  {
    super(ip,timestamp);

    this.percentage = percentage;

    this.load_average = load_average;

    this.process_counts = process_counts;

    this.threads = threads;

    this.io_percent = io_percent;

    this.status = status;
  }

  @Override
  public Tuple getTuple(int discoveryId)
  {
    return Tuple.of(discoveryId, percentage, load_average, process_counts, threads, io_percent, getTimestamp());
  }


  public static Cpu_Metric fromJson(JsonObject object,Timestamp timestamp)
  {
    var ip = object.getString("ip");

    var percentage = Float.valueOf(object.getString("percentage"));

    var load_average = Float.valueOf(object.getString("load_average"));

    var process_counts = Integer.valueOf(object.getString("process_counts"));

    var io_percent = Float.valueOf(object.getString("io_percent"));

    var threads = Integer.valueOf(object.getString("threads"));

    return new Cpu_Metric(ip,timestamp,percentage,load_average,process_counts,threads,io_percent,object.getBoolean("status"));

  }

}


