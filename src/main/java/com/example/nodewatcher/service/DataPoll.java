package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DataPoll extends AbstractVerticle
{

  private long lastMemoryPoll;

  private long lastCpuPoll;

  private static final int POLL_INTERVAL = 100;

  public DataPoll()
  {
    var current_time = System.currentTimeMillis();

    lastCpuPoll = current_time;

    lastMemoryPoll = current_time;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    vertx.eventBus().localConsumer("poll", handler ->
    {

      startPolling();

    });

    startPromise.complete();
  }

  private void startPolling()
  {
    vertx.setPeriodic(POLL_INTERVAL, handler ->
    {
      long currentTime = System.currentTimeMillis();

      pollMetricIfDue(currentTime, lastMemoryPoll, Address.MEMORY_INTERVAL, "memory");

      pollMetricIfDue(currentTime, lastCpuPoll, Address.CPU_INTERVAL, "cpu");

    });
  }

  private void pollMetricIfDue(long currentTime, long lastPollTime, long interval, String metric)
  {
    if (currentTime - lastPollTime >= interval)
    {
      sendMetricData(metric);

      updateLastPollTime(metric, currentTime);

    }
  }

  private void sendMetricData(String metric)
  {
    var data = new JsonObject().put("metric", metric);

    var jsonArray = new JsonArray().add(data);

    vertx.eventBus().send(Address.PLUGIN_DATA_SENDER, jsonArray);

  }

  private void updateLastPollTime(String metric, long currentTime)
  {

    if (metric.equals("memory"))
    {
      lastMemoryPoll = currentTime;
    }
    else if (metric.equals("cpu"))
    {
      lastCpuPoll = currentTime;
    }

  }
}
