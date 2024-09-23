package com.example.nodewatcher.service;

import com.example.nodewatcher.db.CpuDB;
import com.example.nodewatcher.db.MemoryDB;
import com.example.nodewatcher.models.Cpu_Metric;
import com.example.nodewatcher.models.Memory_Metric;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.ZMQ;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;


public class PluginDataSaver extends AbstractVerticle
{

  private final static Logger LOGGER =
    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);


  SqlClient sqlClient;

  public PluginDataSaver(SqlClient sqlClient)
  {
    this.sqlClient =sqlClient;

  }
  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    LogManager lgmngr = LogManager.getLogManager();

    Logger log = lgmngr.getLogger(Logger.GLOBAL_LOGGER_NAME);

    System.out.println("Plugin receiver loaded");

    vertx.eventBus().<String>localConsumer(Address.dumpDB,handler->{
        try
        {

          var decodedBase64Data = Base64.getDecoder().decode(handler.body());

          var deviceDetails = new JsonArray(new String(decodedBase64Data, ZMQ.CHARSET));

          var lenOfMessage = deviceDetails.size();

          var time = (String) deviceDetails.remove(lenOfMessage-1);

          var metric = deviceDetails.remove(lenOfMessage-2);

          log.log(Level.INFO, "Arrived At "+deviceDetails+"Size "+lenOfMessage+" On "+time+" Metric : "+metric);

          deviceDetails.forEach(device->{

            var jsonObeject  = (JsonObject)device;

            if(jsonObeject.getBoolean("status"))
            {

              if(metric.equals("memory"))
              {
                var memory_metric = Memory_Metric.fromJson((JsonObject) device);

                var localDateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                var timestamp = Timestamp.valueOf(localDateTime);

                MemoryDB.saveMemory(sqlClient,memory_metric,timestamp);

              }
              else if(metric.equals("cpu"))
              {
                log.log(Level.INFO,"Arrived in Cpu Saver!!!");
                var cpu_metric = Cpu_Metric.fromJson((JsonObject) device);

                var localDateTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                var timestamp = Timestamp.valueOf(localDateTime);

                log.log(Level.FINE,"Cpu Information :"+cpu_metric);

                CpuDB.save(sqlClient,cpu_metric,timestamp);

              }
            }

            else
              log.log(Level.SEVERE,"Device is Down :"+jsonObeject.getString("ip"));

          });
        }
        catch (Exception exception)
        {

          startPromise.fail("Exception "+exception.getMessage());

        }

    });
  }
}
