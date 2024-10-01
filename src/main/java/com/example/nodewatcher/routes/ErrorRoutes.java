package com.example.nodewatcher.routes;

import io.vertx.ext.web.Router;

public class ErrorRoutes
{
  public static void attach(Router router)
  {

    router.errorHandler(404,pageNotFoundHandler->{

      pageNotFoundHandler.response().end("Page Not Found!");

    });

    router.errorHandler(400,pageNotFoundHandler->{

      pageNotFoundHandler.response().end("Something went wrong! Request Type might be wrong! ");

    });

  }
}
