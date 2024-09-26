package com.example.nodewatcher.db;

import com.example.nodewatcher.models.Cpu_Metric;
import com.example.nodewatcher.models.Memory_Metric;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.sql.Timestamp;

public class MetricDB
{
  public static void saveMemory(SqlClient sqlClient, Memory_Metric memoryMetric, Timestamp timestamp)
  {

    var discoveryIp = memoryMetric.getIp();

    sqlClient.preparedQuery("SELECT id FROM Discovery WHERE ip = ?")

      .execute(Tuple.of(discoveryIp))

      .onComplete(returnRowsResult -> {

        if(returnRowsResult.succeeded())
        {
          returnRowsResult.result().forEach(row->{

            var discovery_id = row.getInteger(0);

            System.out.println("Discovery Id "+ discovery_id);

            sqlClient.preparedQuery("INSERT INTO Memory_Metric(discoveryId,free,swap,used," +
              "cache,disc_used,created_at) VALUES (?,?,?,?,?,?,?)")

              .execute(Tuple.of(discovery_id,memoryMetric.getFree(),memoryMetric.getSwap(),
                memoryMetric.getCached(),memoryMetric.getCached(),memoryMetric.getDisk_space(),timestamp))

              .onComplete(result ->{

                if(result.succeeded())
                {
                  System.out.println("Rows Added in db ");

                }
                else
                {
                  System.out.println("Failed while adding rows "+result.result());
                }

                }).onFailure(failure->{

                System.out.println("Failed last "+failure.getMessage());
              });
          });
        }
      });

  }
  public static void saveCpu(SqlClient sqlClient, Cpu_Metric cpuMetric, Timestamp timestamp)
  {

    System.out.println("In the save cpu "+cpuMetric.getIp()+"sql client :"+sqlClient);

    var discoveryIp = cpuMetric.getIp();

    sqlClient.preparedQuery("SELECT id FROM Discovery WHERE ip = ?")

      .execute(Tuple.of(discoveryIp))

      .onComplete(returnRowsResult -> {

        if(returnRowsResult.succeeded())
        {
          returnRowsResult.result().forEach(row->{

            var discovery_id = row.getInteger(0);

            System.out.println("Discovery Id "+ discovery_id);

            sqlClient.preparedQuery("INSERT INTO CPU_Metric(discoveryId,percentage,load_average,process_counts," +
                "io_percent,threads,created_at) VALUES (?,?,?,?,?,?,?)")

              .execute(Tuple.of(discovery_id,cpuMetric.getPercentage(),cpuMetric.getLoad_average(),
                cpuMetric.getProcess_counts(),cpuMetric.getIo_percent(),cpuMetric.getThreads(),timestamp))

              .onComplete(result ->{

                if(result.succeeded())
                {
                  System.out.println("Rows Added in CPU DB ");

                }
                else
                {
                  System.out.println("Failed while adding rows "+result.result());
                }

              }).onFailure(failure->{

                System.out.println("Failed last "+failure.getMessage());
              });
          });
        }
      });

  }

  public Future<JsonArray> getMemoryMetrics(SqlClient sqlClient, String discoveryName)
  {

    var query = "SELECT m.* FROM Memory_Metric m " +
      "JOIN Discovery d ON m.discoveryId = d.id " +
      "WHERE d.name = ? ORDER BY created_at DESC";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(discoveryName))
      .map(rows -> {
        JsonArray metricsArray = new JsonArray();
        rows.forEach(row -> {
          JsonObject metric = new JsonObject()
            .put("created_at", row.getLocalDateTime("created_at").toString())
            .put("free", row.getLong("free"))
            .put("used", row.getLong("used"))
            .put("swap", row.getLong("swap"))
            .put("disc_used", row.getLong("disc_used"))
            .put("cache", row.getLong("cache"));

          metricsArray.add(metric);
        });
        return metricsArray;
      });
  }

  public Future<JsonArray> getMemoryMetricsLastN(SqlClient sqlClient, String discoveryName, int n)
  {


    var query = "SELECT m.* FROM Memory_Metric m " +
      "JOIN Discovery d ON m.discoveryId = d.id " +
      "WHERE d.name = ? ORDER BY created_at DESC LIMIT ?";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(discoveryName,n))
      .map(rows -> {
        JsonArray metricsArray = new JsonArray();
        rows.forEach(row -> {
          JsonObject metric = new JsonObject()

            .put("created_at", row.getLocalDateTime("created_at").toString())

            .put("free", row.getLong("free"))

            .put("used", row.getLong("used"))

            .put("swap", row.getLong("swap"))

            .put("disc_used", row.getLong("disc_used"))
            .put("cache", row.getLong("cache"));

          metricsArray.add(metric);

        });
        return metricsArray;
      });
  }
  public Future<JsonArray> getCPUMetrics(SqlClient sqlClient, String discoveryName)
  {

    var query = "SELECT m.* FROM CPU_Metric m " +
      "JOIN Discovery d ON m.discoveryId = d.id " +
      "WHERE d.name = ? ORDER BY created_at DESC LIMIT ?";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(discoveryName))
      .map(rows -> {
        JsonArray metricsArray = new JsonArray();
        rows.forEach(row -> {
          JsonObject metric = new JsonObject()
            .put("created_at", row.getLocalDateTime("created_at").toString())
            .put("user_cpu_percentage", row.getFloat("percentage"))
            .put("load_average", row.getFloat("load_average"))
            .put("process", row.getInteger("process_counts"))
            .put("threads", row.getInteger("threads"))
            .put("io_percent", row.getFloat("io_percent"));
          metricsArray.add(metric);
        });
        return metricsArray;
      });
  }
  public Future<JsonArray> getCPUMetricsLastN(SqlClient sqlClient, String discoveryName, int n)
  {


    var query = "SELECT m.* FROM CPU_Metric m " +
      "JOIN Discovery d ON m.discoveryId = d.id " +
      "WHERE d.name = ? ORDER BY created_at DESC LIMIT ?";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(discoveryName,n))
      .map(rows -> {
        JsonArray metricsArray = new JsonArray();
        rows.forEach(row ->
        {
          JsonObject metric = new JsonObject()
            .put("created_at", row.getLocalDateTime("created_at").toString())
            .put("user_cpu_percentage", row.getFloat("percentage"))
            .put("load_average", row.getFloat("load_average"))
            .put("process", row.getInteger("process_counts"))
            .put("threads", row.getInteger("threads"))
            .put("io_percent", row.getFloat("io_percent"));
          metricsArray.add(metric);

        });
        return metricsArray;
      });
  }


}
