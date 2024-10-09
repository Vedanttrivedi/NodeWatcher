package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class PluginDataSender extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(PluginDataSender.class);

  private ZMQ.Socket socket;

  private Boolean pollStarted;

  public PluginDataSender(ZContext context)
  {

    pollStarted = false;

    socket = context.createSocket(SocketType.PUSH);

    socket.bind(Address.PUSH_SOCKET);

  }

  @Override
  public void start(Promise<Void> startPromise)
  {
       vertx.eventBus().<JsonArray>localConsumer(Address.PLUGIN_DATA_SENDER, handler->{

          send(handler.body());

       });

       startPromise.complete();

  }
  private void send(JsonArray data)
  {

      var encodedData = Base64.getEncoder().encode(data.encode().getBytes());

      var status = socket.send(encodedData,ZMQ.DONTWAIT);

      while(!status)
      {

        //log.error("Polling failed ! Plugin is down");

        status = socket.send(encodedData,ZMQ.DONTWAIT);

      }

      if(!pollStarted)
      {
        vertx.eventBus().send("poll","Start Polling");

        pollStarted = true;

      }

    }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception
  {
    socket.close();

    stopPromise.complete();

  }
}
