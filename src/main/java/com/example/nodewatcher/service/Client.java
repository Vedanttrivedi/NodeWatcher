package com.example.nodewatcher.service;

import com.example.nodewatcher.routes.CredentialsRoutes;
import com.example.nodewatcher.routes.DiscoveryRoutes;
import com.example.nodewatcher.routes.ProvisionalRoutes;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;

public class Client extends AbstractVerticle
{

  private SqlClient sqlClient;

  public Client(SqlClient sqlClient)
  {

    this.sqlClient =sqlClient;

  }

  @Override

  public void start(Promise<Void> startPromise) throws Exception
  {

    Router router = Router.router(vertx);

    router.get("/").handler(context1 -> {

      context1.response().
        end("<h1>Connected to LiteNMS </h1>");

    });


    CredentialsRoutes.attach(router,sqlClient);

    ProvisionalRoutes.attach(router, sqlClient);

    vertx.deployVerticle(new DiscoveryRoutes(router,sqlClient));

    System.out.println("Attached discovery routes");

    vertx.createHttpServer()

      .exceptionHandler(handler->{

        System.out.println("Something went wrong "+ handler.getLocalizedMessage());

      })
      .requestHandler(router)

      .listen(Config.HTTP_PORT, http->{

        System.out.println("trying to listen on port "+Config.HTTP_PORT);

        if(http.succeeded())
        {
          System.out.println("Listening on port");

          startPromise.complete();
        }

        else
        {
          System.out.println("Failed to listen on port "+Config.HTTP_PORT);

          startPromise.fail("Not Able to listen on port "+Config.HTTP_PORT);
        }

      });
  }
}
