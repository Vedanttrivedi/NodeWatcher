package com.example.nodewatcher.routes;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.db.MetricDB;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MonitorRoutes
{

  private final MetricDB metricDB;

  private final Router router;

  public MonitorRoutes(Router router)
  {
    this.router = router;

    this.metricDB = new MetricDB(BootStrap.databaseClient);
  }

  public void attach()
  {

    router.get("/:discoveryName/:metricType").handler(this::getMetrics);

    router.get("/:discoveryName/:metricType/range").handler(this::getMetricsInRange);

  }

  private void getMetrics(RoutingContext ctx)
  {

    var discoveryName = ctx.pathParam("discoveryName");

    var metricType = ctx.pathParam("metricType");

    if(metricType.equals("cpu") || metricType.equals("memory"))
    {
      var tableName = metricType.equals("cpu") ?"CPU_Metric":"Memory_Metric";

      metricDB.getMetrics(discoveryName, tableName)
        .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))
        .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));

    }
    else
    {
      ctx.response().end("Invalid metric type! Use cpu or memory instead");

    }
  }

  private void getMetricsInRange(RoutingContext ctx)
  {

    var discoveryName = ctx.pathParam("discoveryName");

    var metricType = ctx.pathParam("metricType");

    var tableName = metricType + "_metric";

    var startTime = ctx.queryParam("startTime").get(0);

    var endTime = ctx.queryParam("endTime").get(0);

    metricDB.getMetricsInRange(discoveryName, tableName, startTime, endTime)
      .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))
      .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
  }
}
