package com.example.nodewatcher.routes;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.db.DiscoveryDB;
import com.example.nodewatcher.service.PluginDataSaver;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.slf4j.LoggerFactory;

public class DiscoveryRoutes extends AbstractVerticle
{

  private final Router router;

  private final DiscoveryDB discoveryDB;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  public DiscoveryRoutes(Router router)
  {
    this.router = router;

    this.discoveryDB = new DiscoveryDB(BootStrap.getDatabaseClient());
  }

  @Override
  public void start(Promise<Void> startPromise)
  {
    startPromise.complete();
    attach();

  }

  public void attach()
  {

    router.post("/discovery/create")
      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(this::createDiscovery);

    router.get("/discovery/get/:name").handler(this::getDiscovery);

    router.get("/discovery/all").handler(this::getAllDiscovery);

    router.put("/discovery/update/:name").
      handler(BodyHandler.create())
      .handler(this::updateDiscovery);

    router.delete("/discovery/delete/:name").handler(this::deleteDiscovery);

    router.patch("/discovery/provision/:name").
      handler(BodyHandler.create()).
      handler(this::provisionDiscovery);

  }

  void getDiscovery(RoutingContext context)
  {

    var name = context.pathParam("name").trim();

    vertx.executeBlocking(promise->{

      discoveryDB.getDiscovery(name)

        .onComplete(result->{

          if(result.succeeded())
          {
            if(result.result()==null)
            {
              promise.fail("Discovery does not exists");
            }
            else
              promise.complete(result.result());
          }
        });

    },future->{
        if(future.succeeded())
        {
          var discovery = (JsonObject)future.result();

          vertx.eventBus().request(Address.SSHCHECK,discovery,reply->{

            discovery.remove("password");

            if(reply.succeeded())
            {

              discovery.put("status","Up");

              context.response().putHeader("Content-Type", "application/json").end(discovery.encodePrettily());

            }
            else
            {
              discovery.put("status","Down");

              context.response().putHeader("Content-Type", "application/json").end(discovery.encodePrettily());

            }
          });
        }
        else
          context.response().end("Discovery not found!");
    });

  }

  void getAllDiscovery(RoutingContext context)
  {

    discoveryDB.getAllDiscoveries()
      .onComplete(result->{

        if(result.succeeded())
          context.response().end(result.result().encodePrettily());

        else
          context.response().end(result.cause().toString());

      });
  }

  void updateDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name");

    var ip = context.request().getFormAttribute("ip");

    if(context.request().getFormAttribute("credential_name")!=null)
    {

      discoveryDB.updateDiscovery(name,ip,context.request().getFormAttribute("credential_name"))
        .onComplete(result->{

          if(result.succeeded())
            context.response().end("Ip and Credential Updated For Discovery "+name);
          else
            context.response().end("Credential_name or Discovery name does not exists");
        });
    }
    else
    {
      discoveryDB.updateDiscovery(name,ip)
        .onComplete(result->{

          if(result.result()==1)
            context.response().end("Ip updated!");
          else
           context.response().end("Credential_name or Discovery name does not exists");

        });
    }

  }

  void deleteDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name");

    discoveryDB.deleteDiscovery(name)
      .onComplete(result->{

        if(result.succeeded())
          context.response().end(result.result());

        else
          context.response().end(result.cause().getMessage());

      });

  }

  void provisionDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name").trim();

    System.out.println("Status : "+context.request().getFormAttribute("status"));

    var status = context.request().getFormAttribute("status");

    if(status==null)
    {
      context.response().end("Provision Status Missing. Provide status = 1 for provision and 0 to remove provision");
      return;
    }
    try
    {
      var doProvision = Integer.valueOf(status) ==1;
      {

        discoveryDB.provisionDiscovery(name,doProvision)

          .onSuccess(success ->
          {

            if(success.rowCount()!=0)
            {
              context.response().end("Discovery Provision Status Updated ");

              if(doProvision)
                logger.info("Polling started for  "+name);
              else
                logger.info("Polling stopped for  "+name);

              var discoveryAndCredentialBody = new JsonObject();

              discoveryDB.getDiscoveryAndCredentialByDiscoveryName(name)

                .onSuccess(

                  rowResult->{

                    discoveryAndCredentialBody.put("username",rowResult.iterator().next().getString(0));

                    discoveryAndCredentialBody.put("password", rowResult.iterator().next().getString(1));

                    discoveryAndCredentialBody.put("ip",rowResult.iterator().next().getString(2));

                    discoveryAndCredentialBody.put("name",rowResult.iterator().next().getString(3));

                    discoveryAndCredentialBody.put("doPolling",doProvision);


                    vertx.eventBus().send(Address.UPDATE_DISCOVERY,discoveryAndCredentialBody);

                  });


            }
            else
            {
              if(doProvision)
                context.response().end("It is already in provision state");
              else
                context.response().end("It is already not in the provision state");
            }

          })
          .onFailure(err -> context.response().end(err.getMessage()));
      }

    }
    catch (Exception exception)
    {
      context.response().end("invalid value for status . Must be 0 or 1");

      logger.error("Error "+exception.getMessage());
    }

  }

  void createDiscovery(RoutingContext context)
  {
    try
    {

      var ip = context.request().getFormAttribute("ip");

      var name = context.request().getFormAttribute("name");

      var credentialName = context.request().getFormAttribute("credential_name");

      discoveryDB.findCredential(credentialName)

      .compose(credential -> {

        if (credential == null)
          return Future.failedFuture("Credential not found");


        credential.put("ip", ip);
        return vertx.eventBus().<JsonObject>request(Address.SSHCHECK, credential,new DeliveryOptions().setSendTimeout(4000))
            .map(reply -> new JsonObject().put("credential", credential).put("sshReply", reply.body()));

        })
        .compose(result -> {

          var credential = result.getJsonObject("credential");

          var sshReply = result.getString("sshReply");

          return discoveryDB.sameIpAndDiscoveryNameExists(ip, name)

        .compose(duplicateRow -> {

          String finalMessage;

          if (duplicateRow.equals("Present"))
          {
            finalMessage = sshReply + " - Discovery with same IP or Name already present!";

            return Future.failedFuture(finalMessage);
          }
          else
          {
            finalMessage = sshReply + " - Saved in DB. You can provision it later.";

            return discoveryDB.createDiscovery(name, ip, credential.getInteger("id")).map(v -> finalMessage);
          }

          });

        })

        .onSuccess(result -> context.response().end(result))

        .onFailure(err -> context.response().end(err.getMessage()));

    }
    catch (Exception exception)
    {
      System.out.println("Error in create discovery "+exception.getMessage());
    }
  }

}
