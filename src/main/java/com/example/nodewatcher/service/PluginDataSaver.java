package com.example.nodewatcher.service;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.db.MetricDB;
import com.example.nodewatcher.models.Cpu_Metric;
import com.example.nodewatcher.models.Memory_Metric;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.*;
import org.zeromq.ZMQ;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;


public class PluginDataSaver extends AbstractVerticle
{
  private static final Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  private final MetricDB metricDB;

  public PluginDataSaver()
  {

    metricDB = new MetricDB(BootStrap.getDatabaseClient());

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    vertx.eventBus().<String>localConsumer(Address.DUMPDB,handler->{

    try
    {

      var device = new JsonObject(new String(handler.body().getBytes()));

      var localDateTime = LocalDateTime.parse(device.getString("time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

      var timestamp = Timestamp.valueOf(localDateTime);

      if(device.getBoolean("status"))
      {

          if(device.getString("metric").equals("memory"))
          {
            var memory_metric = Memory_Metric.fromJson(device);

            metricDB.saveMemory(memory_metric,timestamp)
            .onComplete(result->
            {
                if(result.failed())
                  System.out.println("Db insertion error");
            });

          }

          else
          {
            var cpuMetric = Cpu_Metric.fromJson(device);

            metricDB.saveCpu(cpuMetric,timestamp)
            .onComplete(dbResult->
            {
              if (dbResult.failed())
                System.out.println("Dn error failed");
            });

          }
      }
      else
      {
          logger.error("Device Credential Or Network Issue "+device);
      }

    }

    catch (Exception exception)
    {
      logger.error("Error",exception.getMessage());
    }

  });

    startPromise.complete();

  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception
  {
    super.stop(stopPromise);
  }
}
