package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;

public record Cpu_Metric  (String ip, float percentage, float load_average, int process_counts, float io_percent, int threads, boolean status)
{
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

      return new Cpu_Metric(ip,percentage,load_average,process_counts,io_percent,threads,object.getBoolean("status"));

    }
    catch (Exception e)
    {
      System.out.println("failed for "+object);
    }
    return new Cpu_Metric("ip",0,0,0,0,0,false);
  }

}
