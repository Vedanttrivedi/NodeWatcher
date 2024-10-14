package com.example.nodewatcher.models;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

import java.sql.Timestamp;

public abstract class Metric
{
  private String ip;
  private Timestamp timestamp;

  public Metric(String ip, Timestamp timestamp) {
    this.ip = ip;
    this.timestamp = timestamp;
  }

  public String getIp() {
    return ip;
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public abstract JsonObject toJson();

  public abstract Tuple getTuple(int discoveryId);

}
