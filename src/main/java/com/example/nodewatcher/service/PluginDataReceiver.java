package com.example.nodewatcher.service;
import com.example.nodewatcher.utils.Address;
import com.sun.source.tree.TryTree;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class PluginDataReceiver implements Runnable
{
  private ZMQ.Socket pullSocket;

  private ZContext context;

  private Vertx vertx;

  public PluginDataReceiver(Vertx vertx)
  {

    this.context = new ZContext();

    this.pullSocket = context.createSocket(SocketType.PULL);

    pullSocket.connect("tcp://localhost:4556");

    this.vertx = vertx;
  }


  public void run()
  {

    System.out.println("Plugin Data receiver loaded");
    try
    {
      while (true)
      {

        var message = pullSocket.recvStr();

        System.out.println("Received Message "+message);

        if (message != null)

          vertx.eventBus().send(Address.dumpDB, message);

      }
    }
    catch (Exception exception)
    {
      System.out.println("Exception occured "+exception.getMessage());
    }

  }


}
