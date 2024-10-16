package com.example.nodewatcher.web.routes;

import com.example.nodewatcher.Bootstrap;
import com.example.nodewatcher.database.Metric;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.TimeoutHandler;
import java.util.HashSet;

public class Monitor
{

  private final Router router;

  private static HashSet<String> supportedMetrics = new HashSet<>();

  private Metric metricDB = new Metric(Bootstrap.databaseClient);


  public Monitor(Router router)
  {
    this.router = router;

    supportedMetrics.add("free");

    supportedMetrics.add("cache");

    supportedMetrics.add("disc_used");

    supportedMetrics.add("swap");

    supportedMetrics.add("used");

    supportedMetrics.add("load_average");

    supportedMetrics.add("threads");

    supportedMetrics.add("io_percent");

    supportedMetrics.add("percentage");

    supportedMetrics.add("process_counts");

  }

  public void attach()
  {

    router.get("/:discoveryName/:metricType").handler(this::getMetrics);

    router.get("/:discoveryName/:metricType/:n").handler(this::getLastNMetrics);

    router.get("/:discoveryName/:metricType/:aggr/:secondary_metric")
      .handler(TimeoutHandler.create(3000))
      .handler(this::getMetricAggregation);

  }


  private void getMetrics(RoutingContext ctx)
  {

    var discoveryName = ctx.pathParam("discoveryName");

    var metricType = ctx.pathParam("metricType");

    if(metricType.equals("cpu") || metricType.equals("memory"))
    {
      var tableName = getTableName(metricType);

      metricDB.getMetrics(discoveryName, tableName)
        .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))
        .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));

    }
    else
    {
      ctx.response().end("Invalid metric type! Use cpu or memory instead");

    }
  }

  private void getLastNMetrics(RoutingContext ctx)
  {
    try
    {
      var discoveryName = ctx.pathParam("discoveryName");

      var n = ctx.pathParam("n");

      var tableName = getTableName(ctx.pathParam("metricType"));

      metricDB.getLastNMetrics(discoveryName,tableName,Integer.parseInt(n))

        .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))
        .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));

    }

    catch (NumberFormatException exception)
    {
      ctx.response().end("Invalid Value");
    }

    catch (Exception exception)
    {
      ctx.response().end("Something went wrong! Try again!");
    }
  }

  private void getMetricAggregation(RoutingContext ctx)
  {

    var discoveryName = ctx.pathParam("discoveryName");

    var metricType = ctx.pathParam("metricType");

    var aggr = ctx.pathParam("aggr");

    var secondary_metric = ctx.pathParam("secondary_metric");

    if(!supportedMetrics.contains(secondary_metric))
    {
      ctx.response().end("Invalid Secondary Metric Type!");

      return;
    }

    if(aggr.equals("max") || aggr.equals("avg") || aggr.equals("min"))
    {

      var tableName = getTableName(metricType);

      metricDB.getAggrMetric(discoveryName, tableName,aggr,secondary_metric)

        .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))

        .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));

    }
    else
    {
      ctx.response().end("Invalid metric type! Use cpu or memory instead");

    }
  }

  private  static String getTableName(String memory)
  {
    return memory.equals("memory")?"Memory_Metric":"CPU_Metric";

  }

}
