package com.example.nodewatcher;

import com.example.nodewatcher.db.DatabaseClient;
import com.example.nodewatcher.service.*;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.*;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.ZContext;

public class BootStrap
{

  public static final SqlClient databaseClient = DatabaseClient.getClient();

  public static final Vertx vertx = Vertx.vertx();

  public static final ZContext zContext = new ZContext();

  public static void main(String[] args)
  {

    vertx.deployVerticle(new HostReachabilityChecker())

    .compose(deploymentId->vertx.deployVerticle(new Client()))

    .compose(deploymentId->vertx.deployVerticle(new PluginInitializer()))

    .compose(deploymentId->vertx.deployVerticle(new PluginDataSender()))

    .compose(deploymentId->vertx.deployVerticle(new DataPoll()))

    .compose(deploymentId->vertx.deployVerticle(new UnReachableDiscovery()))

    .onComplete(deploymentResult->{

        if(deploymentResult.succeeded())
        {
          var pluginDataReceiver = new PluginDataReceiver();

          pluginDataReceiver.start();

          vertx.deployVerticle(PluginDataSaver.class.getName(),
          new DeploymentOptions().setInstances(Config.DATA_SAVER_INSTANCES),result->{

            if(result.failed())
                System.out.println("Error "+result.cause().getMessage());

            });

          System.out.println("Verticals Deployed!");

        }
        else
        {
          System.out.println("Something went wrong : "+deploymentResult.cause());
        }
      });

  }
}
