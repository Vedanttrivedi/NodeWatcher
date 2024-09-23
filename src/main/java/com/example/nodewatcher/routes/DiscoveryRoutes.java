package com.example.nodewatcher.routes;

import com.example.nodewatcher.db.DiscoveryDB;
import com.example.nodewatcher.models.Discovery;
import com.example.nodewatcher.utils.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.zeromq.ZMQ;

public class DiscoveryRoutes extends AbstractVerticle
{

  private final Router router;
  private final DiscoveryDB discoveryDB;

  public DiscoveryRoutes(Router router, DiscoveryDB discoveryDB)
  {
    this.router = router;
    this.discoveryDB = discoveryDB;
  }

  @Override
  public void start(Promise<Void> startPromise)
  {
    attach();
  }

  public void attach()
  {

    // Post route to create discovery
    router.post("/discovery/create")
      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(this::createDiscovery);

    // Get details of a particular discovery
    router.get("/discovery/get/:name").handler(this::getDiscovery);

    // Get all discovery records
    router.get("/discovery/all").handler(this::getAllDiscovery);

    // Update a discovery record
    router.put("/discovery/update/:name").handler(this::updateDiscovery);

    // Delete a discovery record
    router.delete("/discovery/delete/:name").handler(this::deleteDiscovery);

    // Provision discovery (polling logic)
    router.patch("/discovery/provision/:name").handler(this::provisionDiscovery);


  }

  // Helper method to find the credential ID by credential name
  void findCredentialId(String credentialName, Promise<Integer> promise)
  {
    discoveryDB.findCredentialId(credentialName)
      .onSuccess(promise::complete)
      .onFailure(promise::fail);
  }

  // Handler to create a new discovery record
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
        System.out.println("Duplicate Row "+duplicateRow.trim());
        if (duplicateRow.equals("Remove"))
        {
          context.response().end(duplicateRow);
          return null;
        }
        else
        {
          vertx.eventBus().request(Address.pingCheck, data, reply ->
          {
            if (reply.succeeded())
              context.response().end("<h1>" + reply.result().body() + ", Saved in DB. You can provision it later.</h1>");
             else
              context.response().end("Device is Down");

          });

          return discoveryDB.createDiscovery(name, ip, credentialId);
        }
      })
    ).onFailure(err -> context.response().setStatusCode(404).end(err.getMessage()));
  }

  // Handler to get details of a specific discovery by name
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
        vertx.eventBus().request(Address.pingCheck,discovery.toJson("Down"),reply->{

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

  // Handler to delete a discovery record
  void deleteDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name");
    discoveryDB.deleteDiscovery(name)
      .onSuccess(success -> context.response().setStatusCode(200).end("Discovery deleted successfully."))
      .onFailure(err -> context.response().setStatusCode(500).end("Failed to delete discovery: " + err.getMessage()));
  }

  // Handler to provision discovery
  void provisionDiscovery(RoutingContext context) {
    var name = context.pathParam("name").trim();

    //once the provision request comes , check if the discovery with that name exists
    //if it does then send it to plugin

    discoveryDB.provisionDiscovery(name)
      .onSuccess(success ->
      {
        context.response().end("Discovery Provisioned");

        var discoveryAndCredentialBody = new JsonObject();

        discoveryDB.getDiscoveryAndCredentialByDiscoveryName(name)
          .onSuccess(rowResult->{

              discoveryAndCredentialBody.put("username",rowResult.iterator().next().getString(0));

              discoveryAndCredentialBody.put("password",rowResult.iterator().next().getString(1));

              discoveryAndCredentialBody.put("ip",rowResult.iterator().next().getString(2));

              discoveryAndCredentialBody.put("name",rowResult.iterator().next().getString(3));

            System.out.println("About to send "+discoveryAndCredentialBody);

            vertx.eventBus().send(Address.pluginDataSender,discoveryAndCredentialBody);

            })
          .onFailure(failureHandler->{

            System.out.println("Error While Fetching discovery with other options ");

            });

      })
      .onFailure(err -> context.response().end("Not in the provision state " + err.getMessage()));

  }
}
