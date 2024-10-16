package com.example.nodewatcher.service;

import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnReachableDiscovery extends AbstractVerticle
{

  private static final Logger log = LoggerFactory.getLogger(UnReachableDiscovery.class);

  private final Map<String, JsonObject> unreachedMonitors;

  public UnReachableDiscovery()
  {
    unreachedMonitors = new ConcurrentHashMap<>();
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {

    vertx.eventBus().<JsonObject>localConsumer(Address.UNREACHED_DISCOVERY,handler->{

      unreachedMonitors.put(handler.body().getString("ip"),handler.body());

    });

    //Every 5 Minutes try reaching devices which are down

    //If Device gets up send to plugin sender and remove from unReachedMonitors

    vertx.setPeriodic(Address.UNREACHBILITY_TIMER,handler->{

      //start iteration over all unreached monitored

      if(!unreachedMonitors.isEmpty())
      {

        unreachedMonitors.forEach((ip,device)->
        {

          var data = new JsonObject();

          data.put("ip", ip);

          vertx.eventBus().request(Address.PINGCHECK, data, reply ->
          {

            if(reply.succeeded())
            {

              vertx.eventBus().send(Address.UPDATE_DISCOVERY, device);

              log.info("Discovery in reach " + device.getString("ip"));

              unreachedMonitors.remove(ip);

            }
            else
            {

              System.out.println("Still Down!");

            }
          });
        });
      }

      else
        System.out.println("No unreached device found!");

    });

    startPromise.complete();

  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception
  {

    super.stop(stopPromise);

  }
}
