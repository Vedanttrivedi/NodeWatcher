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
      .onComplete(deploymentResult->{
        if(deploymentResult.succeeded())
        {
          log.error("Deployment Failed");

          var pluginDataReceiverThread = new PluginDataReceiver(context,vertx);

          pluginDataReceiverThread.start();

          vertx.deployVerticle(new PluginDataSaver(databaseClient),

            dataSaverResult->{

              if(dataSaverResult.failed())
                System.out.println("Failed to Deploy Data saver!");

            });
        }
      });

      vertx.eventBus().localConsumer("close",handler->{

        System.out.println("Closing the application ");

        System.exit(1);

      });
    }
}
