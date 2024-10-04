package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.AbstractCollection;
import java.util.Base64;

public class PluginDataSender extends AbstractVerticle
{
  private static final Logger log = LoggerFactory.getLogger(PluginDataSender.class);
  private ZMQ.Socket pushSocket;

  private Boolean pollStarted;

  private JsonArray data;

  private Boolean isSent;

  public PluginDataSender(ZContext context)
  {

    pollStarted = false;

    this.pushSocket = context.createSocket(SocketType.PUSH);

    this.pushSocket.bind(Address.PUSH_SOCKET);

    this.data = new JsonArray();

    this.isSent = false;

  }

  @Override
  public void start(Promise<Void> startPromise)
  {
       vertx.eventBus().<JsonArray>localConsumer("send", handler->{

          this.data = handler.body();

          isSent = true;

          send();

       });

       startPromise.complete();

  }
  private void send()
  {
      if(isSent)
      {

        var encodedData = Base64.getEncoder().encode(data.encode().getBytes());

        var status = pushSocket.send(encodedData,ZMQ.DONTWAIT);

        System.out.println("Status "+status);

        if(!status)
        {
          log.error("Plugin not started ");

        }
        else
        {
          isSent = false;

          if(!pollStarted)
          {
            vertx.eventBus().send("poll","Start Polling");

            pollStarted = true;

          }
        }
      }

    }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception
  {
    pushSocket.close();

    stopPromise.complete();

  }
}
