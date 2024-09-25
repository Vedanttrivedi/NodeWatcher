package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;

public class Cpu_Metric extends Metric
{
  private float percentage;
  private float load_average;
  private int process_counts;
  private int threads;
  private float io_percent;
  private boolean status;


  public Cpu_Metric(String ip, float percentage, float load_average, int process_counts, int threads, float io_percent, boolean status)
  {
    super(ip);

    this.percentage = percentage;

    this.load_average = load_average;

    this.process_counts = process_counts;

    this.threads = threads;

    this.io_percent = io_percent;

    this.status = status;
  }

  public float getPercentage()
  {
    return percentage;
  }

  public float getLoad_average()
  {
    return load_average;
  }

  public int getProcess_counts()
  {
    return process_counts;
  }

  public int getThreads()
  {
    return threads;
  }

  public float getIo_percent()
  {
    return io_percent;
  }

  public boolean isStatus()
  {
    return status;
  }

  public static Cpu_Metric fromJson(JsonObject object)
  {
    try
    {
      var ip = object.getString("ip");

      var percentage = Float.valueOf(object.getString("percentage"));

      var load_average = Float.valueOf(object.getString("load_average"));

      var process_counts = Integer.valueOf(object.getString("process_counts"));

      var io_percent = Float.valueOf(object.getString("io_percent"));

      var threads = Integer.valueOf(object.getString("threads"));

      return new Cpu_Metric(ip,percentage,load_average,process_counts,threads,io_percent,object.getBoolean("status"));

    }
    catch (Exception e)
    {
      System.out.println("failed for "+object);
    }
    return new Cpu_Metric("ip",0,0,0,0,0,false);
  }
}


