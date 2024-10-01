package com.example.nodewatcher.service;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class PluginDataReceiver extends Thread
{
  private ZMQ.Socket pullSocket;

  private Vertx vertx;

  public PluginDataReceiver(ZContext context, Vertx vertx)
  {


    this.pullSocket = context.createSocket(SocketType.PULL);

    pullSocket.connect(Address.PULL_SOCKET);

    this.vertx = vertx;
  }

  @Override
  public void run()
  {
    try
    {
      while (true)
      {
        var message = pullSocket.recvStr();

        if (message != null)
          vertx.eventBus().send(Address.DUMPDB, message);

      }
    }
    catch (Exception exception)
    {
      System.out.println("Exception occured "+exception.getMessage());
    }

  }
}
