package com.example.nodewatcher.service;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class PluginDataReceiver extends AbstractVerticle
{
  private ZMQ.Socket pullSocket;



  public PluginDataReceiver(ZContext context)
  {


    this.pullSocket = context.createSocket(SocketType.PULL);

    pullSocket.connect(Address.PULLSOCKET);

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    startPromise.complete();

    starter();

  }

  public void starter()
  {
    System.out.println("Receiver Loaded");
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
