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


    CredentialsRoutes.attach(router,sqlClient);

    ProvisionalRoutes.attach(router, sqlClient);

    vertx.deployVerticle(new DiscoveryRoutes(router,sqlClient));

    vertx.createHttpServer()

      .exceptionHandler(handler->{

        System.out.println("Something went wrong "+ handler.getLocalizedMessage());

        startPromise.fail(handler.getCause());

      })
      .requestHandler(router)

      .listen(Config.HTTP_PORT, http->{

        if(http.succeeded())
        {
          startPromise.complete();
        }

        else
        {
            startPromise.fail("Not Able to listen on port "+Config.HTTP_PORT);
        }

      });
  }
}
