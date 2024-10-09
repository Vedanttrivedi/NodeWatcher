package com.example.nodewatcher;

import com.example.nodewatcher.db.DatabaseClient;
import com.example.nodewatcher.service.*;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.*;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.ZContext;


public class BootStrap
{

  static SqlClient databaseClient;

  public static SqlClient getDatabaseClient()
  {
    return databaseClient;
  }

  public static void main(String[] args)
  {
    var vertx = Vertx.vertx();

    databaseClient = DatabaseClient.getClient(vertx);

    var context = new ZContext();

    var pluginDataReceiverThread = new PluginDataReceiver(context,vertx);

    pluginDataReceiverThread.start();

    vertx.deployVerticle(new HostReachabilityChecker())

    .compose(deploymentId->vertx.deployVerticle(new Client()))

    .compose(deploymentId->vertx.deployVerticle(new PluginInitializer()))

    .compose(deploymentId->vertx.deployVerticle(new PluginDataSender(context)))

    .compose(deploymentId->vertx.deployVerticle(new DataPoll()))

    .compose(deploymentId->vertx.deployVerticle(PluginDataSaver.class.getName(),
      new DeploymentOptions().setInstances(Config.DATA_SAVER_INSTANCES)))

    .compose(deploymentId->vertx.deployVerticle(new UnReachableDiscovery()))

    .onComplete(deploymentResult->{

        if(deploymentResult.succeeded())
        {
          System.out.println("Verticals Deployed!");
        }
        else
        {
          System.out.println("Something went wrong : "+deploymentResult.cause());
        }
      });

    }
}
