package com.example.nodewatcher.service;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.Vertx;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class PluginDataReceiver implements Runnable
{
  private ZMQ.Socket pullSocket;

  private ZContext context;

  private Vertx vertx;

  public PluginDataReceiver(Vertx vertx,ZContext context)
  {

    this.context = context;

    this.pullSocket = context.createSocket(SocketType.PULL);

    pullSocket.connect(Address.pullSocket);

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
