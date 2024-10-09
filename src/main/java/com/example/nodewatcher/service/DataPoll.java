package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;

public class DataPoll extends AbstractVerticle
{
  private long lastMemoryPoll;

  private long lastCpuPoll;

  public DataPoll()
  {
    var current_time = System.currentTimeMillis();

    lastCpuPoll =current_time;

    lastMemoryPoll =  current_time;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    vertx.eventBus().localConsumer("poll",handler->{

      startPolling();

    });

    startPromise.complete();

  }

  private void startPolling()
  {

    vertx.setPeriodic(100, handler ->
    {

      long currentTime = System.currentTimeMillis();

      if (currentTime - lastMemoryPoll >= Address.MEMORY_INTERVAL)
      {
          System.out.println("Memory polling "+ LocalDateTime.now().toString());

          var data = new JsonObject();

          data.put("metric", "memory");

          var jsonArray = new JsonArray();

          jsonArray.add(data);

          vertx.eventBus().send("send",jsonArray);

          lastMemoryPoll = currentTime;

      }

      if (currentTime - lastCpuPoll >= Address.CPU_INTERVAL)
      {

          System.out.println("Cpu polling "+ LocalDateTime.now().toString());

          var data = new JsonObject();

          data.put("metric", "cpu");

          var jsonArray = new JsonArray();

          jsonArray.add(data);

          vertx.eventBus().send("send",jsonArray);

          lastCpuPoll = currentTime;

      }

    });

  }
}
