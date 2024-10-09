package com.example.nodewatcher.routes;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.db.MetricDB;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
public class ProvisionalRoutes
{

  private final MetricDB provisionDB;

  private final Router router;

  public ProvisionalRoutes(Router router)
  {
    this.router = router;

    provisionDB = new MetricDB(BootStrap.getDatabaseClient());

  }

  public void attach()
  {
    router.get("/discovery/memory/:discoveryName")
      .handler(this::getMemoryMetrics);

    router.get("/discovery/memory/:discoveryName/:n")
      .handler(this::getMemoryMetricsLastN);

    router.get("/discovery/cpu/:discoveryName")
      .handler(this::getCPUMetrics);

    router.get("/discovery/cpu/:discoveryName/:n")
      .handler(this::getCPUMetricsLastN);

    router.get("/discovery/cpu/:discoveryName/aggr")

      .handler(this::getMemoryMetricsAggr);

  }
  private void getMemoryMetricsAggr(RoutingContext ctx)
  {

    var aggr  = ctx.request().getParam("aggr");

    var metricName = ctx.request().getParam("metric");

    provisionDB.getMemoryMetricsAggr( aggr,metricName)

    .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))

    .onFailure(err -> ctx.response().end("Error while fetching data "));

  }

  private void getMemoryMetrics(RoutingContext ctx)
  {
    var discoveryName = ctx.pathParam("discoveryName");

    provisionDB.getMemoryMetrics(discoveryName)

    .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))

    .onFailure(err -> ctx.response().end("Error while fetching data "));

  }

  private void getMemoryMetricsLastN(RoutingContext ctx)
  {

    try
    {

      var discoveryName = ctx.pathParam("discoveryName");

      var n = ctx.pathParam("n");

      provisionDB.getMemoryMetricsLastN(discoveryName, Integer.parseInt(n))

      .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))

      .onFailure(err -> ctx.response().end(err.getMessage()));

    }

    catch (Exception exception)
    {
      ctx.response().setStatusCode(400).end("Invalid time parameter");
    }

  }

  private void getCPUMetrics(RoutingContext ctx)
  {
    var discoveryName = ctx.pathParam("discoveryName");

    try
    {
      provisionDB.getCPUMetrics(discoveryName)

      .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))

      .onFailure(err -> ctx.response().end("Error while fetching data "));

    }
    catch (Exception exception)
    {
      ctx.response().end("Error "+exception.getMessage());
    }

  }

  private void getCPUMetricsLastN(RoutingContext ctx)
  {
    try
    {

      var discoveryName = ctx.pathParam("discoveryName");

      var n = ctx.pathParam("n");

      provisionDB.getCPUMetricsLastN(discoveryName, Integer.parseInt(n))

      .onSuccess(metrics -> ctx.response().end(metrics.encodePrettily()))

      .onFailure(err -> ctx.response().end(err.getMessage()));

    }

    catch (Exception exception)
    {
      ctx.response().setStatusCode(400).end("Invalid time parameter");
    }

  }
}
