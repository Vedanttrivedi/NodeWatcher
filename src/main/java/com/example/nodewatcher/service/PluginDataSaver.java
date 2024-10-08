package com.example.nodewatcher.service;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.db.DatabaseClient;
import com.example.nodewatcher.db.MetricDB;
import com.example.nodewatcher.models.Cpu_Metric;
import com.example.nodewatcher.models.Memory_Metric;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.*;
import org.zeromq.ZMQ;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;


public class PluginDataSaver extends AbstractVerticle
{

  private static final Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  private static SqlClient sqlClient;

  public PluginDataSaver()
  {

    sqlClient = BootStrap.getDatabaseClient();

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    vertx.eventBus().<String>localConsumer(Address.DUMPDB,handler->{

      try
        {

          var decodedBase64Data = Base64.getDecoder().decode(handler.body());

          var device = new JsonObject(new String(decodedBase64Data, ZMQ.CHARSET));

              var devicesInfo  = (JsonObject) device;

              if(devicesInfo.getString("metric").equals("memory"))
              {

                if(devicesInfo.getBoolean("status"))
                {
                  var memory_metric = Memory_Metric.fromJson(device);

                  var localDateTime = LocalDateTime.parse(devicesInfo.getString("time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                  var timestamp = Timestamp.valueOf(localDateTime);

                  vertx.executeBlocking(saveInDbFuture-> {


                    MetricDB.saveMemory(sqlClient,memory_metric,timestamp)
                      .onComplete(result->
                      {
                          if(result.succeeded())
                            saveInDbFuture.complete();
                          else
                            saveInDbFuture.fail("DB Insertion error ");
                      });

                  },saveInDbFutureRes->{

                    if(saveInDbFutureRes.failed())
                        logger.error("DB(Memory) Save error "+saveInDbFutureRes.cause());

                  });
                }
                else
                {
                  logger.error("Device Credential Or Network Issue "+device);

                }

              }
              else if(devicesInfo.getString("metric").equals("cpu"))
              {

                if(devicesInfo.getBoolean("status"))
                {
                  var cpuMetric = Cpu_Metric.fromJson((JsonObject) device);

                  var localDateTime = LocalDateTime.parse(devicesInfo.getString("time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                  var timestamp = Timestamp.valueOf(localDateTime);

                  vertx.executeBlocking(saveInDbPromise-> {

                    MetricDB.saveCpu(sqlClient,cpuMetric,timestamp).
                      onComplete(dbResult->{

                        if(dbResult.succeeded())
                          saveInDbPromise.complete();
                        else
                          saveInDbPromise.fail(dbResult.cause());

                      });

                  },saveInDbFuture->{

                    if(saveInDbFuture.failed())
                      logger.error("DB(CPU) Save error "+saveInDbFuture.cause());

                  });

                }
                else
                {
                  logger.error("Device Credential Or Network Issue "+device);

                }
              }

        }
        catch (Exception exception)
        {
          logger.error("error ",exception.getMessage());
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
