package com.example.nodewatcher.service;

import com.example.nodewatcher.routes.CredentialsRoutes;
import com.example.nodewatcher.routes.DiscoveryRoutes;
import com.example.nodewatcher.routes.ErrorRoutes;
import com.example.nodewatcher.routes.ProvisionalRoutes;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;

public class Client extends AbstractVerticle
{

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    var router = Router.router(vertx);

    var credentialRoutes = new CredentialsRoutes(router);

    credentialRoutes.attach();

    var provisionalRoutes = new ProvisionalRoutes(router);

    provisionalRoutes.attach();

    vertx.deployVerticle(new DiscoveryRoutes(router),

      handler->{

      if(handler.failed())
        System.out.println("Failed Deploying Discovery Routes");

    });

    ErrorRoutes.attach(router);

    vertx.createHttpServer()

      .exceptionHandler(handler->{

        System.out.println("Something went wrong "+ handler.getLocalizedMessage());

        startPromise.fail(handler.getCause());

      })
      .requestHandler(router)

      .listen(Config.HTTP_PORT, http->{

        if(http.succeeded())
        {

          System.out.println("Sever started on port "+ Config.HTTP_PORT);

          startPromise.complete();
        }

        else
        {
            startPromise.fail("Not Able to listen on port "+Config.HTTP_PORT);
        }

      });
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception
  {
    super.stop(stopPromise);
  }
}
