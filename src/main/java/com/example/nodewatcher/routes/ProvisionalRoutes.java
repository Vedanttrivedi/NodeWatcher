package com.example.nodewatcher.routes;

import com.example.nodewatcher.db.MetricDB;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.SqlClient;

public class ProvisionalRoutes
{

  private static final MetricDB provisionDB = new MetricDB();

  public static void attach(Router router, SqlClient sqlClient)
  {
    router.get("/discovery/memory/:discoveryName")
      .handler(ctx -> getMemoryMetrics(ctx, sqlClient));

    router.get("/discovery/memory/:discoveryName/:n")
      .handler(ctx -> getMemoryMetricsLastN(ctx, sqlClient));

    router.get("/discovery/cpu/:discoveryName")
      .handler(ctx -> getCPUMetrics(ctx, sqlClient));

    router.get("/discovery/cpu/:discoveryName/:n")
      .handler(ctx -> getCPUMetricsLastN(ctx, sqlClient));

  }

  private static void getMemoryMetrics(RoutingContext ctx, SqlClient sqlClient)
  {
    var discoveryName = ctx.pathParam("discoveryName");

    provisionDB.getMemoryMetrics(sqlClient, discoveryName)
      .onSuccess(metrics -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(metrics.encode()))

      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .end("Error fetching memory metrics: " + err.getMessage()));

  }

  private static void getMemoryMetricsLastN(RoutingContext ctx, SqlClient sqlClient)
  {
    String discoveryName = ctx.pathParam("discoveryName");

    var nParam = ctx.pathParam("n");

    int n;

    try
    {
      n = Integer.parseInt(nParam);
    }
    catch (NumberFormatException e)
    {
      ctx.response().setStatusCode(400).end("Invalid time parameter");
      return;
    }

    provisionDB.getMemoryMetricsLastN(sqlClient, discoveryName,n)
      .onSuccess(metrics -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(metrics.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .end("Error fetching memory metrics: " + err.getMessage()));

  }
  private static void getCPUMetrics(RoutingContext ctx, SqlClient sqlClient)
  {
    var discoveryName = ctx.pathParam("discoveryName");

    provisionDB.getCPUMetrics(sqlClient, discoveryName)
      .onSuccess(metrics -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(metrics.encodePrettily()))

      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .end("Error fetching memory metrics: " + err.getMessage()));

  }

  private static void getCPUMetricsLastN(RoutingContext ctx, SqlClient sqlClient)
  {
    String discoveryName = ctx.pathParam("discoveryName");

    var nParam = ctx.pathParam("n");

    int minute;

    try
    {
      minute = Integer.parseInt(nParam);
    }
    catch (NumberFormatException e)
    {
      ctx.response().setStatusCode(400).end("Invalid time parameter");
      return;
    }

    provisionDB.getCPUMetricsLastN(sqlClient, discoveryName,minute)
      .onSuccess(metrics -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(metrics.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .end("Error fetching memory metrics: " + err.getMessage()));

  }


}
