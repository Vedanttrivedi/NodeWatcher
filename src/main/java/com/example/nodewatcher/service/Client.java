package com.example.nodewatcher.service;

import com.example.nodewatcher.routes.CredentialsRoutes;
import com.example.nodewatcher.routes.DiscoveryRoutes;
import com.example.nodewatcher.routes.MonitorRoutes;
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

    var credentialRouter = Router.router(vertx);

    var credentialRoutes = new CredentialsRoutes(credentialRouter);

    credentialRoutes.attach();

    var discoveryRouter = Router.router(vertx);

    var discoveryRoutes  = new DiscoveryRoutes();

    discoveryRoutes.attach(discoveryRouter);

    var monitorRouter = Router.router(vertx);

    var monitorRoutes = new MonitorRoutes(monitorRouter);

    monitorRoutes.attach();

    router.route("/credentials/*").subRouter(credentialRouter);

    router.route("/discovery/*").subRouter(discoveryRouter);

    router.route("/monitor/*").subRouter(monitorRouter);

    vertx.createHttpServer()

      .exceptionHandler(handler -> startPromise.fail(handler.getCause()))

      .requestHandler(router)

      .listen(Config.HTTP_PORT, http ->
      {

        if (http.succeeded())
        {
          startPromise.complete();
        }
        else
        {
          startPromise.fail("Not Able to listen on port " + Config.HTTP_PORT);
        }

      });

  }
}
