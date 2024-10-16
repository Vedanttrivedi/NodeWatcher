package com.example.nodewatcher.service;

import com.example.nodewatcher.Bootstrap;
import com.example.nodewatcher.utils.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

public class PluginDataReceiver extends Thread
{
  private ZMQ.Socket pullSocket;

  private static final Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  public PluginDataReceiver()
  {

    this.pullSocket = Bootstrap.zContext.createSocket(SocketType.PULL);

    pullSocket.connect(Address.PULL_SOCKET);

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
        {
          Bootstrap.vertx.eventBus().send(Address.DUMPDB, message);

        }
      }

    }
    catch (Exception exception)
    {
      logger.error("Not Receiving data from plugin ");

    }

  }
}
