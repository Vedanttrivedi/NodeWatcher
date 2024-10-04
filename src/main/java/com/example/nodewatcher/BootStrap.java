package com.example.nodewatcher;

import com.example.nodewatcher.db.DatabaseClient;
import com.example.nodewatcher.service.*;
import io.vertx.core.*;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;

public class BootStrap
{
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(BootStrap.class);

  public static void main(String[] args)
  {

    var vertx = Vertx.vertx();

    var databaseClient = DatabaseClient.getClient(vertx);

    var context = new ZContext();

    vertx.deployVerticle(new HostReachabilityChecker())

    .compose(deploymentId->vertx.deployVerticle(new Client(databaseClient)))

    .onComplete(deploymentResult->{
        if(deploymentResult.failed())
        {
          log.error("Deployment Failed");
        }
      });

    vertx.deployVerticle(new PluginInitializer(databaseClient))

      .compose(deploymentId->vertx.deployVerticle(new PluginDataSender(context)))

      .compose(deploy->vertx.deployVerticle(new DataPoll()))

      .onComplete(deploymentResult->{

        if(deploymentResult.succeeded())
        {

          var pluginDataReceiverThread = new PluginDataReceiver(context,vertx);
          pluginDataReceiverThread.start();

          var pluginDataReceiverThread2 = new PluginDataReceiver2(context,vertx);
          pluginDataReceiverThread2.start();

          Future.join(
            vertx.deployVerticle(new PluginDataSaver(databaseClient)),

            vertx.deployVerticle(new PluginDataSaver(databaseClient)),

            vertx.deployVerticle(new PluginDataSaver(databaseClient)),

            vertx.deployVerticle(new PluginDataSaver(databaseClient))
            )

            .onComplete(result->{
             if(result.failed())
               System.out.println("Something went wrong while deploying data saver "+result.cause());
          });

        }
      });

    }
}
