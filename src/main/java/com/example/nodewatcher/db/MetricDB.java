package com.example.nodewatcher.db;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.models.Cpu_Metric;
import com.example.nodewatcher.models.Memory_Metric;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import com.example.nodewatcher.models.Metric;

public class MetricDB
{
  private final  SqlClient sqlClient;

  public MetricDB(SqlClient sqlClient)
  {
    this.sqlClient = sqlClient;
  }

  public  Future<Boolean> save(Metric metric)
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

  private  String getInsertQuery(Metric metric)
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

  public  Future<JsonArray> getMetrics(String discoveryName, String tableName)
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

  public  Future<JsonArray> getLastNMetrics(String discoveryName, String tableName, int n)
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

  public  Future<JsonObject> getAggrMetric(String discoveryName,String tableName,String aggr,String metric)
  {
    try
    {
      Promise<JsonObject> promiseData = Promise.promise();

      var futureOfData = promiseData.future();

      sqlClient.preparedQuery("SELECT id FROM Discovery WHERE name = ? ")

        .execute(Tuple.of(discoveryName))

        .compose(result->{

          var query = queryMaker(tableName,aggr,metric);

          System.out.println("Sql query "+query);

          var discoveryID = result.iterator().next().getInteger(0);


          return sqlClient.preparedQuery("")
            .execute(Tuple.of(discoveryID,discoveryID));
        })
        .onSuccess(handler->{

          var row = handler.iterator().next();

          var payLoad = new JsonObject();

          payLoad.put("discoveryId",row.getInteger(0));

          payLoad.put(aggr,row.getInteger(1));

          promiseData.complete(payLoad);

        })

        .onFailure(handler->{
          System.out.println(handler.getCause());
          promiseData.fail(handler.getCause());

        });

      return futureOfData;

    }
    catch (Exception exception)
    {
      return Future.failedFuture("OOPS "+exception.getMessage());

    }
  }

  private  String queryMaker(String tableName,String aggr,String secondary_metric)
  {


    return "SELECT discoveryId,"+ secondary_metric+", created_at" +
      " FROM " + tableName+
      " WHERE discoveryId = ? " +
      " AND "+ secondary_metric +
      " = (SELECT " +aggr+"("+secondary_metric+") " +
      " FROM "+ tableName+
      " WHERE discoveryId = ? )";

  }
}
