package com.example.nodewatcher;

import com.example.nodewatcher.db.DatabaseClient;
import com.example.nodewatcher.service.*;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.*;
import io.vertx.mysqlclient.MySQLPool;
import org.zeromq.ZContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BootStrap
{
  private static final Logger LOGGER = Logger.getLogger(BootStrap.class.getName());

  public static void main(String[] args)
  {

    var vertx = Vertx.vertx(
      new VertxOptions()

        .setWorkerPoolSize(Config.WORKER_POOL_SIZE)

        .setMaxWorkerExecuteTime(Config.WORKER_THREAD_MAX_EXECUTION_TIME)

        .setMaxWorkerExecuteTimeUnit(Config.TIME_UNIT)
    );

    var databaseClient = DatabaseClient.getClient(vertx);

    var context = new ZContext();

    deployInitialVerticles(vertx, databaseClient, context)

      .compose(initialDeploymentResult -> deployPluginDataReceiverAndSaver(vertx, databaseClient, context))

      .onComplete(deploymentResult ->
      {

          if (deploymentResult.succeeded())
          {
            LOGGER.log(Level.INFO, "All verticles deployed successfully");
          }
          else
          {
            LOGGER.log(Level.SEVERE, "Failed to deploy verticles", deploymentResult.cause());
          }

        });
    }

  private static Future<Void> deployInitialVerticles(Vertx vertx, MySQLPool databaseClient, ZContext context)
  {
    return Future.all
      (
      vertx.deployVerticle(new MainVertical(databaseClient)),

      vertx.deployVerticle(new PingVertical()),

      vertx.deployVerticle(

        new PluginDataSender(databaseClient, context),

        new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER))
    ).
      mapEmpty();

  }

  private static Future<Void> deployPluginDataReceiverAndSaver(Vertx vertx, MySQLPool databaseClient, ZContext context)
  {

    return Future.all
      (
      vertx.deployVerticle(
        new PluginDataReceiver(context),

        new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)),

        vertx.deployVerticle(new PluginDataSaver(databaseClient))
    ).mapEmpty();

  }

}
