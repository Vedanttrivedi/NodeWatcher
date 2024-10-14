package com.example.nodewatcher.routes;

import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorRoutes
{
  private static final Logger logger = LoggerFactory.getLogger(ErrorRoutes.class);

  public  void attach(Router router)
  {

    router.errorHandler(404, context -> {
      logger.error("404 Error on path: " + context.request().path());
      context.response().end("Page Not Found!");
    });

    router.errorHandler(500, context -> {
      logger.error("500 Error: ", context.failure());
      context.response().end("Internal Server Error");
    });

  }
}
