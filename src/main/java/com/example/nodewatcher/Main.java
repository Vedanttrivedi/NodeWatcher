package com.example.nodewatcher;

import com.example.nodewatcher.db.DatabaseClient;
import com.example.nodewatcher.service.*;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.impl.JsonUtil;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.SqlClient;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.*;
import java.util.concurrent.TimeUnit;


public class Main
{
  static Logger LOGGER =
    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  public static void main(String[] args)
  {

    var vertx = Vertx.vertx(
      new VertxOptions()
      .setWorkerPoolSize(30).
        setMaxWorkerExecuteTime(70).
      setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS)
    );

    SqlClient client = DatabaseClient.getClient(vertx);

    ZContext context;

    ZMQ.Socket socket;

    ZMQ.Socket pluginReceiverSocket;

    LogManager lgmngr = LogManager.getLogManager();

    Logger log = lgmngr.getLogger(Logger.GLOBAL_LOGGER_NAME);

    vertx.deployVerticle(new PluginDBDumper(client));

    try
    {
      context = new ZContext();

      socket = context.createSocket(SocketType.PUSH);

      socket.bind("tcp://localhost:4555");

      pluginReceiverSocket = context.createSocket(SocketType.PULL);

      pluginReceiverSocket.connect("tcp://localhost:4556");

      vertx.deployVerticle(new PluginVertical(client,socket),pluginLoaded->{

        if(pluginLoaded.succeeded())
        {

          vertx.deployVerticle(new PluginCpu(socket));

          vertx.deployVerticle(new PluginMemory(socket));

          vertx.deployVerticle(new PingVertical());

          vertx.deployVerticle(new MainVertical(client,socket),

            handler ->
            {
              if (handler.succeeded())
                log.log(Level.INFO,"Plugin loaded");

              else
                log.log(Level.SEVERE,handler.cause().toString()+" Main vertical ");

            });
        }
        else
          log.log(Level.SEVERE,pluginLoaded.cause().toString()+" Plugin vertical ");

      });



      while (true)
      {
        System.out.println("Loader ");

        var dataOfMetric = pluginReceiverSocket.recvStr();

        vertx.eventBus().send(Address.dumpDB,dataOfMetric);

      }
    }
    catch (Exception exception)
    {

      log.log(Level.SEVERE,exception.toString()+" Main Application ");

    }
  }

}
