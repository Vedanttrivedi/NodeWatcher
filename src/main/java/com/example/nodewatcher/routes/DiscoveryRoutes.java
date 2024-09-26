package com.example.nodewatcher.routes;

import com.example.nodewatcher.db.DiscoveryDB;
import com.example.nodewatcher.service.PluginDataSaver;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

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

  void findCredentialId(String credentialName, Promise<Integer> promise)
  {
    discoveryDB.findCredentialId(credentialName)
      .onSuccess(promise::complete)
      .onFailure(promise::fail);
  }

  void createDiscovery(RoutingContext context)
  {

    var ip = context.request().getFormAttribute("ip");

    var name = context.request().getFormAttribute("name");

    var credentialName = context.request().getFormAttribute("credential_name");

    var data = new JsonObject().put("ip", ip).put("name", name);

    Promise<Integer> credentialIdPromise = Promise.promise();

    Promise<String> discoveryIpNamePromise = Promise.promise();

    findCredentialId(credentialName, credentialIdPromise);

    discoveryDB.sameIpAndDiscoveryNameExists(ip, name, discoveryIpNamePromise);

    credentialIdPromise.future().compose(credentialId ->
      discoveryIpNamePromise.future().compose(duplicateRow ->
      {
        if (duplicateRow.equals("Remove"))
        {
          context.response().end(duplicateRow);

          return null;
        }
        else
        {
          vertx.eventBus().request(Address.PINGCHECK, data, reply ->
          {
            if (reply.succeeded())
            {
              context.response().end("<h1>" + reply.result().body() + ", Saved in DB. You can provision it later.</h1>");

              logger.error("Discovery in reach ",reply.cause());

            }
            else
            {
              context.response().end("Device is Down");

              logger.error("Discovery not in reach ",reply.cause());

            }
          });

          return discoveryDB.createDiscovery(name, ip, credentialId);
        }
      })
    ).onFailure(err -> context.response().setStatusCode(404).end(err.getMessage()));
  }

  private Future<Void> pingAndSaveDiscovery(RoutingContext context, JsonObject data, String name, String ip, Integer credentialId)
  {
    Promise<Void> promise = Promise.promise();

    vertx.eventBus().request(Address.PINGCHECK, data, new DeliveryOptions().setSendTimeout(5000), reply ->
    {
      if (reply.succeeded())
      {
        discoveryDB.createDiscovery(name, ip, credentialId)
          .onSuccess(v -> {

            context.response().end("<h1>" + reply.result().body() + ", Saved in DB. You can provision it later.</h1>");


            promise.complete();
          })
          .onFailure(err -> {
            context.response().end("Error saving to database: " + err.getMessage());
            promise.fail(err);
          });
      }
      else
      {
        context.response().end("Device is down! Cannot add. Try again!");
        promise.complete();
      }
    });

    return promise.future();
  }

  void getDiscovery(RoutingContext context)
  {

    var name = context.pathParam("name").trim();

    discoveryDB.getDiscovery(name)
      .onSuccess(discovery ->
      {
        if(discovery==null)
        {
          context.response().end("Discovery not found");
          return;
        }
        //Once user is getting all the discovery he should be able to get the result whether device is reachable or not
        vertx.eventBus().request(Address.PINGCHECK,discovery.toJson("Down"),reply->{

          if(reply.succeeded())
          {
            context.response().putHeader("Content-Type", "application/json").end(discovery.toJson("Up").encodePrettily());

          }
          else
          {
            context.response().putHeader("Content-Type", "application/json").end(discovery.toJson("Down").encodePrettily());

          }
        });

      })
      .onFailure(err -> context.response().setStatusCode(500).end("DB ERROR: " + err.getMessage()));

  }

  // Handler to get all discoveries
  void getAllDiscovery(RoutingContext context)
  {
    discoveryDB.getAllDiscoveries()

      .onSuccess(response -> {

        context.response().putHeader("Content-Type", "application/json").end(response.encodePrettily());

      })
      .onFailure(err -> context.response().setStatusCode(500).end("Failed to fetch discoveries: " + err.getMessage()));

  }

  // Handler to update a discovery record
  void updateDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name");
    var ip = context.request().getFormAttribute("ip");

    discoveryDB.updateDiscovery(name, ip)
      .onSuccess(success -> context.response().end("Discovery IP Updated for " + name))
      .onFailure(err -> context.response().end("Discovery name not found!"));
  }

  void deleteDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name");

    discoveryDB.deleteDiscovery(name)
      .onSuccess(success -> context.response().setStatusCode(200).end("Discovery deleted successfully."))

      .onFailure(err -> context.response().setStatusCode(500).end("Failed to delete discovery: " + err.getMessage()));

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
      discoveryDB.provisionDiscovery(name,doProvision)

        .onSuccess(success ->
        {

          if(success.rowCount()!=0)
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

                  System.out.println("About to send "+discoveryAndCredentialBody);

                  vertx.eventBus().send(Address.PLUGINDATASENDER,discoveryAndCredentialBody);

                  System.out.println("Row Updation Result "+rowResult.rowCount()+"\t"+rowResult.size()+"\t Do Provision Result "+doProvision);

                })

              .onFailure(failureHandler->{

                System.out.println("Error While Fetching discovery with other options ");

                context.response().end(failureHandler.getCause().toString());

              });

          }
          else
            context.response().end("Discovery does not exists");

        })
        .onFailure(err -> context.response().end("Not in the provision state " + err.getMessage()));

    }

    catch (Exception exception)
    {
      context.response().end("invalid value for status . Must be 0 or 1");

      logger.error("Error "+exception.getMessage());
      return;
    }

  }
}
