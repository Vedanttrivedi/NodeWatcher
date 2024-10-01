package com.example.nodewatcher.models;

import java.sql.Timestamp;

public abstract class Metric
{
  protected String ip;

  public Metric(String ip)
  {
    this.ip = ip;
  }

  public String getIp()
  {
    return ip;
  }

}
