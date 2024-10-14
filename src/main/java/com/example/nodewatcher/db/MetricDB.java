package com.example.nodewatcher.db;

import com.example.nodewatcher.models.Cpu_Metric;
import com.example.nodewatcher.models.Memory_Metric;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import com.example.nodewatcher.models.Metric;

import java.util.concurrent.atomic.AtomicReference;

public class MetricDB
{

  private final SqlClient sqlClient;

  public MetricDB(SqlClient sqlClient)
  {

    this.sqlClient = sqlClient;

  }

  public Future<Boolean> save(Metric metric)
  {

    Promise<Boolean> promise = Promise.promise();

    var discoveryIp = metric.getIp();

    sqlClient.preparedQuery("SELECT id FROM Discovery WHERE ip = ?")
      .execute(Tuple.of(discoveryIp))

      .onComplete(result -> {

        if (result.succeeded())
        {
          var discoveryId = result.result().iterator().next().getInteger(0);

          sqlClient.preparedQuery(getInsertQuery(metric))

            .execute(metric.getTuple(discoveryId))

            .onSuccess(res ->{

              promise.complete(true);

            })

            .onFailure(promise::fail);

        }
        else
        {
          promise.fail(result.cause());
        }
      });

    return promise.future();
  }

  private String getInsertQuery(Metric metric)
  {

    if (metric instanceof Memory_Metric)
    {
      return "INSERT INTO Memory_Metric (discoveryId, free, used, swap, cache, disc_used, created_at) VALUES (?,?,?,?,?,?,?)";
    }
    else if (metric instanceof Cpu_Metric)
    {
      return "INSERT INTO CPU_Metric (discoveryId, percentage, load_average, process_counts, threads, io_percent, created_at) VALUES (?,?,?,?,?,?,?)";
    }
    return null;  //invalid metric type
  }

  public Future<JsonArray> getMetrics(String discoveryName, String tableName)
  {

    var query = "SELECT * FROM " + tableName + " WHERE discoveryId = (SELECT id FROM Discovery WHERE name = ?) ORDER BY created_at DESC";

    return sqlClient.preparedQuery(query)

      .execute(Tuple.of(discoveryName))
      .map(rows -> {

        var metricsArray = new JsonArray();

        rows.forEach(row -> metricsArray.add(row.toJson()));

        return metricsArray;

      });

  }

  public Future<JsonArray> getMetricsInRange(String discoveryName, String tableName, String startTime, String endTime)
  {
    var query = "SELECT * FROM " + tableName + " WHERE discoveryId = (SELECT id FROM Discovery WHERE name = ?) AND created_at BETWEEN ? AND ?";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(discoveryName, startTime, endTime))
      .map(rows -> {

        var metricsArray = new JsonArray();

        rows.forEach(row -> metricsArray.add(row.toJson()));

        return metricsArray;

      });
  }

  public Future<JsonArray> getLastNMetrics(String discoveryName, String tableName, int n)
  {

    var query = "SELECT * FROM " + tableName + " WHERE discoveryId = (SELECT id FROM Discovery WHERE name = ?)  limit ? ";

    return sqlClient.preparedQuery(query)

      .execute(Tuple.of(discoveryName, n))

      .map(rows -> {

        var metricsArray = new JsonArray();

        rows.forEach(row -> {

          var metric = row.toJson();

          metric.remove("discoveryId");

          metricsArray.add(metric);

        });

        return metricsArray;

      });
  }

  public Future<JsonObject> getAggrMetric(String discoveryName,String tableName,String aggr,String metric)
  {
    Promise<JsonObject> promiseData = Promise.promise();

    var futureOfData = promiseData.future();

    AtomicReference<String> query = new AtomicReference<>("");

    sqlClient.preparedQuery("SELECT id FROM Discovery WHERE name = ? ")

    .execute(Tuple.of(discoveryName))

    .compose(result->{

      if(aggr.equals("max"))
        query.set("SELECT discoveryId,MAX("+metric+") FROM " +tableName+ " WHERE discoveryId = ? GROUP by discoveryId");

      else if(aggr.equals("avg"))
        query.set("SELECT discoveryId,AVG("+metric+")  FROM " +tableName+ " WHERE discoveryId = ? GROUP by discoveryId");

      else
        query.set("SELECT discoveryId,MIN("+metric+")  FROM " +tableName+ " WHERE discoveryId = ? GROUP by discoveryId");

      return sqlClient.preparedQuery(query.toString())
        .execute(Tuple.of(result.iterator().next().getInteger(0)));
    })
    .onSuccess(handler->{

      var row = handler.iterator().next();

      var payLoad = new JsonObject();

      payLoad.put("discoveryId",row.getInteger(0));

      payLoad.put(aggr,row.getInteger(1));

      promiseData.complete(payLoad);

    })
    .onFailure(handler->{

      promiseData.fail("Invalid query");

    });

    return futureOfData;
  }

}
