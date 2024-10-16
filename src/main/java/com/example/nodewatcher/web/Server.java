package com.example.nodewatcher.web;

import com.example.nodewatcher.web.routes.Credentials;
import com.example.nodewatcher.web.routes.Discovery;
import com.example.nodewatcher.web.routes.Monitor;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;

public class Server extends AbstractVerticle
{

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    var router = Router.router(vertx);

    var credentialRouter = Router.router(vertx);

    var credentialRoutes = new Credentials(credentialRouter);

    credentialRoutes.attach();

    var discoveryRouter = Router.router(vertx);

    var discoveryRoutes  = new Discovery();

    discoveryRoutes.attach(discoveryRouter);

    var monitorRouter = Router.router(vertx);

    var monitorRoutes = new Monitor(monitorRouter);

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
