package com.example.nodewatcher.service;

import com.example.nodewatcher.db.DiscoveryDB;
import com.example.nodewatcher.routes.CredentialsRoutes;
import com.example.nodewatcher.routes.DiscoveryRoutes;
import com.example.nodewatcher.routes.ProvisionalRoutes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.ext.web.Router;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.ZMQ;

public class MainVertical extends AbstractVerticle
{
  final static int port = 4500;

  private SqlClient sqlClient;

  private DiscoveryDB discoveryDB;

  public MainVertical(SqlClient sqlClient)
  {
    this.sqlClient =sqlClient;

    discoveryDB = new DiscoveryDB(sqlClient);
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

    vertx.deployVerticle(new DiscoveryRoutes(router,discoveryDB));

    System.out.println("Attached discovery routes");

    vertx.createHttpServer()

      .exceptionHandler(handler->{

        System.out.println("Something went wrong "+ handler.getLocalizedMessage());

      })
      .requestHandler(router)

      .listen(port,http->{

        System.out.println("trying to listen on port "+port);

        if(http.succeeded())
        {
          System.out.println("Listening on port");

          startPromise.complete();
        }

        else
        {
          System.out.println("Failed to listen on port "+port);

          startPromise.fail("Not Able to listen on port "+port);
        }

      });
  }
}
