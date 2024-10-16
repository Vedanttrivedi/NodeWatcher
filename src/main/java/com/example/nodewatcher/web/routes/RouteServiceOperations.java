package com.example.nodewatcher.web.routes;

import io.vertx.ext.web.RoutingContext;

interface RouteServiceOperations
{

  public void create(RoutingContext context);

  public void update(RoutingContext context);

  public void delete(RoutingContext context);

  public void get(RoutingContext context);

  public void getAll(RoutingContext context);

}
