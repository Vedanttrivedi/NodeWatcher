package com.example.nodewatcher.service;

import com.example.nodewatcher.db.MetricDB;
import com.example.nodewatcher.models.Cpu_Metric;
import com.example.nodewatcher.models.Memory_Metric;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.sql.SQLOutput;
import java.util.logging.Level;
import java.util.logging.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;


public class PluginDataSaver extends AbstractVerticle
{

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  SqlClient sqlClient;

  public PluginDataSaver(SqlClient sqlClient)
  {
    this.sqlClient =sqlClient;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    var logManager = LogManager.getLogManager();

    var log = logManager.getLogger(Logger.GLOBAL_LOGGER_NAME);

    vertx.eventBus().<String>localConsumer(Address.DUMPDB,handler->{

      try
        {
          var decodedBase64Data = Base64.getDecoder().decode(handler.body());

          var deviceDetails = new JsonArray(new String(decodedBase64Data, ZMQ.CHARSET));

          var lenOfMessage = deviceDetails.size();

          var time = (String) deviceDetails.remove(lenOfMessage-1);

          var metric = deviceDetails.remove(lenOfMessage-2);

          deviceDetails.forEach(device->{

              var devicesInfo  = (JsonObject) device;

              if(metric.equals("memory"))
              {

                if(devicesInfo.getBoolean("status"))
                {
                  var memory_metric = Memory_Metric.fromJson((JsonObject) device);

                  var localDateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                  var timestamp = Timestamp.valueOf(localDateTime);

                  MetricDB.saveMemory(sqlClient,memory_metric,timestamp);

                  ((JsonObject) device).remove("password");

                }
                else
                {
                  System.out.println("Could not collect information at "+LocalDateTime.now().toString());

                  logger.error("Device Credential Or Network Issue "+device);

                }

              }
              else if(metric.equals("cpu"))
              {

                if(devicesInfo.getBoolean("status"))
                {
                  var cpuMetric = Cpu_Metric.fromJson((JsonObject) device);

                  var localDateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                  var timestamp = Timestamp.valueOf(localDateTime);

                  log.log(Level.FINE,"Cpu Information :"+cpuMetric);

                  MetricDB.saveCpu(sqlClient,cpuMetric,timestamp);

                  ((JsonObject) device).remove("password");


                }
                else
                {
                  System.out.println("Could not collect information at " + LocalDateTime.now().toString());

                  logger.error("Device Credential Or Network Issue "+device);

                }
              }

          });
        }
        catch (Exception exception)
        {
          logger.error("error",exception);
        }

    });

    startPromise.complete();

  }
}
