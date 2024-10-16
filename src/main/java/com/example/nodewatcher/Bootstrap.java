package com.example.nodewatcher;

import com.example.nodewatcher.database.DatabaseClient;
import com.example.nodewatcher.service.*;
import com.example.nodewatcher.utils.Config;
import com.example.nodewatcher.web.Server;
import io.vertx.core.*;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.ZContext;

public class Bootstrap
{
  public static final SqlClient databaseClient = DatabaseClient.getClient();

  public static final Vertx vertx = Vertx.vertx();

  public static final ZContext zContext = new ZContext();

  public static void main(String[] args)
  {

      vertx.deployVerticle(new Server())

      .compose(deploymentId -> vertx.deployVerticle(new PluginDataSender()))

      .compose(deploymentId -> vertx.deployVerticle(new DataPoll()))

      .compose(deploymentId -> vertx.deployVerticle(new UnReachableDiscovery()))

      .onComplete(deploymentResult ->
      {
        if (deploymentResult.succeeded())
        {

          new PluginDataReceiver().start();

          vertx.deployVerticle(PluginDataSaver.class.getName(),

            new DeploymentOptions().setInstances(Config.DATA_SAVER_INSTANCES), result ->
            {

              if (result.failed())
                System.out.println("Error " + result.cause().getMessage());

            });
        }
        else
        {
          System.out.println("Something went wrong : " + deploymentResult.cause());
        }
      });

  }
}
