package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;

public class DataPoll extends AbstractVerticle
{
  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    vertx.eventBus().localConsumer("poll",handler->{

      startPolling();

    });

    vertx.eventBus().<String>localConsumer("stopPolling",handler->{


    });

    startPromise.complete();

  }
  private void stopPolling(long setPeriodicId)
  {

    vertx.cancelTimer(setPeriodicId);

  }

  private void startPolling()
  {

    final long[] lastMemoryPoll = {System.currentTimeMillis()};
    final long[] lastCpuPoll = {System.currentTimeMillis()};

    vertx.setPeriodic(100, handler ->
    {

      long currentTime = System.currentTimeMillis();

      if (currentTime - lastMemoryPoll[0] >= Address.MEMORY_INTERVAL)
      {
          System.out.println("Memory polling "+ LocalDateTime.now().toString());

          var data = new JsonObject();

          data.put("metric", "memory");

          var jsonArray = new JsonArray();

          jsonArray.add(data);

          vertx.eventBus().send("send",jsonArray);

          lastMemoryPoll[0] = currentTime; // Update last memory poll time

      }

      if (currentTime - lastCpuPoll[0] >= Address.CPU_INTERVAL)
      {

          System.out.println("Cpu polling "+ LocalDateTime.now().toString());

          var data = new JsonObject();

          data.put("metric", "cpu");

          var jsonArray = new JsonArray();

          jsonArray.add(data);

          vertx.eventBus().send("send",jsonArray);

          lastCpuPoll[0] = currentTime; // Update last CPU poll time

      }

    });

  }
}
