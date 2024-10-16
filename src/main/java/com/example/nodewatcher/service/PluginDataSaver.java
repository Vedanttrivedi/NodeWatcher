package com.example.nodewatcher.service;

import com.example.nodewatcher.Bootstrap;
import com.example.nodewatcher.database.Metric;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PluginDataSaver extends AbstractVerticle
{
  private static final Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  private final Metric metricDB = new Metric(Bootstrap.databaseClient);


  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    vertx.eventBus().<String>localConsumer(Address.DUMPDB, handler ->
    {
      try
      {

        var device = new JsonObject(new String(handler.body().getBytes()));

        var localDateTime = LocalDateTime.parse(device.getString("time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        var timestamp = Timestamp.valueOf(localDateTime);

        if (device.getBoolean("status"))
        {
          var metric = com.example.nodewatcher.models.Metric.fromJson(device, timestamp);

          metricDB.save(metric)

            .onComplete(result ->
            {

              if (result.failed())
              {

                System.out.println("Db result error " + result.cause());
              }
            });

        }
        else
        {
          logger.error("Device Credential or Network Issue: " + device);
        }

      }
      catch (Exception exception)
      {
        logger.error("Error", exception);
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
