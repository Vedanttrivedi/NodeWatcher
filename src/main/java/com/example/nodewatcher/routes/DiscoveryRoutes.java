package com.example.nodewatcher.routes;

import com.example.nodewatcher.db.DiscoveryDB;
import com.example.nodewatcher.service.PluginDataSaver;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.LoggerFactory;


public class DiscoveryRoutes extends AbstractVerticle
{

  private final Router router;

  private final DiscoveryDB discoveryDB;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  public DiscoveryRoutes(Router router, SqlClient databaseClient)
  {
    this.router = router;

    this.discoveryDB = new DiscoveryDB(databaseClient);
  }

  @Override
  public void start(Promise<Void> startPromise)
  {
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

    router.put("/discovery/update/:name").handler(this::updateDiscovery);

    router.delete("/discovery/delete/:name").handler(this::deleteDiscovery);

    router.patch("/discovery/provision/:name").
      handler(BodyHandler.create()).
      handler(this::provisionDiscovery);

  }

  void findCredential(String credentialName, Promise<JsonObject> promise)
  {
    vertx.executeBlocking(findPromise->
    {
      discoveryDB.findCredential(credentialName)
        .onComplete(findResult->{
          if(findResult.succeeded())
            findPromise.complete();
          else
            findResult.failed();
      });

    },findFuture->
    {

      if(findFuture.succeeded())
        promise.complete();

      else
        promise.fail(findFuture.cause());
    });

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

            if(reply.succeeded())
            {

              discovery.put("status","Up");

              discovery.remove("password");

              context.response().putHeader("Content-Type", "application/json").end(discovery.encodePrettily());

            }
            else
            {
              discovery.put("status","Down");

              discovery.remove("password");

              context.response().putHeader("Content-Type", "application/json").end(discovery.encodePrettily());

            }
          });
        }
        else
          context.response().end("Discovery not found!");
    });

  }

  // Handler to get all discoveries
  void getAllDiscovery(RoutingContext context)
  {
    vertx.executeBlocking(promise->{

      discoveryDB.getAllDiscoveries()
        .onComplete(result->{

          if(result.succeeded())
            promise.complete(result.result());
          else
            promise.fail(result.cause());

        });
    },future->{

      if(future.succeeded())
        context.response().end(((JsonArray)future.result()).encodePrettily());
      else
        context.response().end(future.cause().toString());
    });
  }

  // Handler to update a discovery record
  void updateDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name");

    var ip = context.request().getFormAttribute("ip");

    vertx.executeBlocking(updatePromise->
    {
      discoveryDB.updateDiscovery(name, ip)
        .onSuccess(success -> updatePromise.complete())
        .onFailure(err -> updatePromise.fail("Discovery name not found!"));

    },updateFuture->{

      if(updateFuture.succeeded())
        context.response().end("Discovery IP Updated for " + name);

      else
        context.response().end(updateFuture.cause().toString());
    });

  }

  void deleteDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name");

    vertx.executeBlocking(deletePromise->{
      discoveryDB.deleteDiscovery(name)
        .onSuccess(success ->{
          System.out.println("Deleted "+success.toString());

          deletePromise.complete("Deletion Successful");
        })

        .onFailure(err -> {
          System.out.println("Delete promise failed"+err.getCause());
          deletePromise.fail(err.getCause());
        });

    },deleteFuture->
    {
      if(deleteFuture.succeeded())
        context.response().end(deleteFuture.result().toString());

      else
        context.response().end(deleteFuture.cause().toString());
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

      vertx.executeBlocking(promise->{

        discoveryDB.provisionDiscovery(name,doProvision)

          .onSuccess(success ->
          {

            if(success.size()!=0 || success.rowCount()!=0)
            {
              context.response().end("Discovery Provision Status Updated ");

              var discoveryAndCredentialBody = new JsonObject();

              discoveryDB.getDiscoveryAndCredentialByDiscoveryName(name)

                .onSuccess(

                  rowResult->{

                    discoveryAndCredentialBody.put("username",rowResult.iterator().next().getString(0));

                    discoveryAndCredentialBody.put("password",rowResult.iterator().next().getString(1));

                    discoveryAndCredentialBody.put("ip",rowResult.iterator().next().getString(2));

                    discoveryAndCredentialBody.put("name",rowResult.iterator().next().getString(3));

                    discoveryAndCredentialBody.put("doPolling",doProvision);

                    vertx.eventBus().send(Address.PLUGIN_DATA_SENDER,discoveryAndCredentialBody);

                  })

                .onFailure(failureHandler->{
                    promise.fail("DB Error");

                });

            }
            else
            {
              if(doProvision)
                promise.fail("It is already in provision state");
              else
                promise.fail("It is already not in the provision state");
            }

          })
          .onFailure(err -> promise.fail("Not in the provision state "));

      },future->{
          if(future.succeeded())
          {
            context.response().end(future.result().toString());
          }
          else
            context.response().end(future.cause().toString());
      });

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

      vertx.executeBlocking(promise -> {

        discoveryDB.findCredential(credentialName).compose(credential -> {

            if (credential == null) {
              promise.fail("Credential not found");
              return Future.failedFuture("Credential not found");
            }

            credential.put("ip", ip);
            return vertx.eventBus().<JsonObject>request(Address.SSHCHECK, credential,new DeliveryOptions().setSendTimeout(4000))
              .map(reply -> new JsonObject().put("credential", credential).put("sshReply", reply.body()));

          }).compose(result -> {

            var credential = result.getJsonObject("credential");

            var sshReply = result.getString("sshReply");

            return discoveryDB.sameIpAndDiscoveryNameExists(ip, name).compose(duplicateRow -> {

              String finalMessage;
              if (duplicateRow.equals("Present"))
              {
                finalMessage = sshReply + " - Discovery with same IP or Name already present!";

                promise.complete(finalMessage);  // Stop here if duplicate is found

                return Future.failedFuture("duplicate data entry");
              }
              else
              {
                // Save the discovery if no duplicate exists
                finalMessage = sshReply + " - Saved in DB. You can provision it later.";

                return discoveryDB.createDiscovery(name, ip, credential.getInteger("id")).map(v -> finalMessage);
              }

            });

          }).onSuccess(result -> promise.complete(result))

          .onFailure(err -> promise.fail(err.getMessage()));

      }, res -> {
        if (res.succeeded())
        {
          context.response().end("<h1>" + res.result() + "</h1>");
        }
        else
        {
          context.response().setStatusCode(404).end(res.cause().getMessage());
        }
      });
    }
    catch (Exception exception)
    {

    }
  }


}
