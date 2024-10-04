package com.example.nodewatcher.routes;

import com.example.nodewatcher.db.MetricDB;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.SqlClient;

public class ProvisionalRoutes extends AbstractVerticle
{

  private final MetricDB provisionDB = new MetricDB();
  private Router router;
  private SqlClient sqlClient;

  public ProvisionalRoutes(Router router, SqlClient sqlClient)
  {
    this.router = router;
    this.sqlClient = sqlClient;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    super.start(startPromise);

    attach();
  }

  public void attach()
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

  private void getMemoryMetrics(RoutingContext ctx, SqlClient sqlClient)
  {
    var discoveryName = ctx.pathParam("discoveryName");

    vertx.executeBlocking(promise ->
    {
      provisionDB.getMemoryMetrics(sqlClient, discoveryName)
        .onSuccess(metrics -> promise.complete(metrics.encodePrettily()))

        .onFailure(err -> promise.fail("Error while fetching data "));

    }, future ->
    {

      if (future.succeeded())
        ctx.response().end(future.result().toString());
      else
        ctx.response().end(future.cause().getMessage());

    });

  }

  private void getMemoryMetricsLastN(RoutingContext ctx, SqlClient sqlClient)
  {

    try
    {

      var discoveryName = ctx.pathParam("discoveryName");

      var n = ctx.pathParam("n");

      vertx.executeBlocking(promise ->
      {

        provisionDB.getMemoryMetricsLastN(sqlClient, discoveryName, Integer.parseInt(n))
          .onSuccess(metrics -> promise.complete(metrics.encodePrettily()))
          .onFailure(err -> promise.fail(err.getMessage()));


      }, future ->
      {

        if (future.succeeded())
          ctx.response().end(future.result().toString());

        else
          ctx.response().end(future.cause().toString());

      });
    }

    catch (Exception exception)
    {
      ctx.response().setStatusCode(400).end("Invalid time parameter");
    }

  }

  private void getCPUMetrics(RoutingContext ctx, SqlClient sqlClient)
  {
    var discoveryName = ctx.pathParam("discoveryName");

    vertx.executeBlocking(promise ->
    {
      provisionDB.getCPUMetrics(sqlClient, discoveryName)

        .onSuccess(metrics -> promise.complete(metrics.encodePrettily()))

        .onFailure(err -> promise.fail("Error while fetching data "));

    }, future ->
    {

      if (future.succeeded())
        ctx.response().end(future.result().toString());
      else
        ctx.response().end(future.cause().getMessage());

    });
  }

  private void getCPUMetricsLastN(RoutingContext ctx, SqlClient sqlClient)
  {

    try
    {

      var discoveryName = ctx.pathParam("discoveryName");

      var n = ctx.pathParam("n");

      vertx.executeBlocking(promise ->
      {

        provisionDB.getCPUMetricsLastN(sqlClient, discoveryName, Integer.parseInt(n))
          .onSuccess(metrics -> promise.complete(metrics.encodePrettily()))
          .onFailure(err -> promise.fail(err.getMessage()));


      }, future ->
      {

        if (future.succeeded())
          ctx.response().end(future.result().toString());

        else
          ctx.response().end(future.cause().toString());

      });
    }

    catch (Exception exception)
    {
      ctx.response().setStatusCode(400).end("Invalid time parameter");
    }

  }
}
