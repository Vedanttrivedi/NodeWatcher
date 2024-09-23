package com.example.nodewatcher;

import com.example.nodewatcher.db.DatabaseClient;
import com.example.nodewatcher.service.*;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.mysqlclient.MySQLPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main
{
  private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

  public static void main(String[] args)
  {
    Vertx vertx = setupVertx();

    MySQLPool databaseClient = DatabaseClient.getClient(vertx);

    try
    {

      Thread thread = new Thread(new PluginDataReceiver(vertx));

      thread.start();

      Future.join(

          vertx.deployVerticle(new PingVertical()),

          vertx.deployVerticle(new MainVertical(databaseClient)),

          vertx.deployVerticle(new PluginDataSender(databaseClient)),

          vertx.deployVerticle(new PluginDataSaver(databaseClient))

        ).
        onComplete(verticalDeploymentResult->{

          if(verticalDeploymentResult.succeeded())
          {
            LOGGER.log(Level.FINER,"All the verticals are deployed");
          }
          else
            LOGGER.log(Level.SEVERE,"Something went wrong while deploying vertical "+verticalDeploymentResult.cause());

        });
    }

    catch (Exception exception)
    {
        LOGGER.log(Level.SEVERE,"Exception "+exception.getMessage());
    }

  }

  private static Vertx setupVertx()
  {

    return Vertx.vertx(new VertxOptions()
      .setWorkerPoolSize(30)
      .setMaxWorkerExecuteTime(70)
      .setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS)
    );

  }

}
