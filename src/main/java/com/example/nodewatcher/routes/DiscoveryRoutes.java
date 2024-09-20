package com.example.nodewatcher.routes;

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
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.Row;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscoveryRoutes extends AbstractVerticle {

  private final Router router;
  private final SqlClient sqlClient;
  private ZMQ.Socket socket;
  //taking socket to send updated device
  public DiscoveryRoutes(Router router, SqlClient sqlClient,ZMQ.Socket socket)
  {
    this.router = router;
    this.sqlClient = sqlClient;
    this.socket = socket;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    attach();
  }

  public void attach()
  {

    // Post route to create discovery
    router.post("/discovery/create")
      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())  // To handle POST request body
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

  static void homeDiscovery(RoutingContext context) {
    context.response().end("<h1>Welcome to the discovery section!</h1>");
  }

  // Helper method to find the credential ID by credential name
  void findCredentialId(String credentialName, Promise<Integer> promise)
  {

    System.out.println("I am checking "+credentialName);

    sqlClient
      .preparedQuery("SELECT id FROM Credentials WHERE name= ? ")

      .execute(Tuple.of(credentialName.trim()))

      .onComplete(result ->
      {
          if(result.succeeded())
          {
            result.result().forEach(row->
            {
              if(row!=null)
              {
                promise.complete(row.getInteger(0));
                System.out.println("Found!");
                return;
              }
            });


          }
          else
            promise.fail("Did not found");
      })
      .onFailure(failure->
      {

        System.out.println("failure "+failure.getMessage());

      })

      .onFailure(err -> promise.fail("Failed to fetch credential: " + err.getMessage()));
  }

  // Handler to create a new discovery record
  void createDiscovery(RoutingContext context)
  {

    var ip = context.request().getFormAttribute("ip");

    var name = context.request().getFormAttribute("name");

    var credentialName = context.request().getFormAttribute("credential_name");

    // First, find the credential ID from the credential name
    System.out.println("Route hit "+ip+"\t"+name+"\t"+credentialName);

    Promise<Integer> credentialIdPromise = Promise.promise();

    findCredentialId(credentialName, credentialIdPromise);

    credentialIdPromise.future().onComplete(ar -> {

      if (ar.succeeded())
      {
        int credentialId = ar.result();

        if(credentialId < 0)
        {
          context.response().end("Credential name does not exists");
          return;
        }
        var data = new JsonObject();

        data.put("ip", ip);

        data.put("name", name);

        data.put("credentialID", credentialId);

        // Send the data to the event bus for ping checking
        vertx.eventBus().request(Address.pingCheck, data, reply ->
        {

          if (reply.succeeded())
          {
            context.response().end("<h1>" + reply.result().body() + ", Saved in DB. You can provision it later.</h1>");
          }
          else
          {
            context.response().end("Something went wrong!");
          }

        });

        // Save the discovery record in the database
        sqlClient
          .preparedQuery("INSERT INTO Discovery (name, ip, credentialID) VALUES (?, ?, ?)")
          .execute(Tuple.of(name, ip, credentialId))
          .onSuccess(rows -> {
            System.out.println("Data added in DB");
          })
          .onFailure(err -> {
            System.out.println("Data is not added in DB: " + err.getMessage());
          });
      } else {
        context.response().setStatusCode(404).end(ar.cause().getMessage());
      }
    });
  }

  // Handler to get details of a specific discovery by name
  void getDiscovery(RoutingContext context)
  {

    var name = context.pathParam("name");

    sqlClient
      .preparedQuery("SELECT * FROM Discovery WHERE name = ?")

      .execute(Tuple.of(name))

      .onSuccess(rows ->
      {
        if (rows.rowCount() == 0)
        {
          context.response().setStatusCode(404).end("Discovery not found.");
        }
        else
        {
          var row = rows.iterator().next();
          var discovery = new Discovery();
          discovery.setId(row.getInteger("id"));
          discovery.setName(row.getString("name"));
          discovery.setCredentialId(row.getInteger("credentialID"));
          discovery.setProvisioned(row.getBoolean("is_provisioned"));
          discovery.setCreated_at(row.getString("created_at"));

          context.response().putHeader("Content-Type", "application/json")
            .end(discovery.toJson().encodePrettily());
        }
      })
      .onFailure(err -> {
        context.response().setStatusCode(500).end("Failed to fetch discovery: " + err.getMessage());
      });
  }

  void getAllDiscovery(RoutingContext context)
  {
    System.out.println("Request for all discovery");

    sqlClient
      .query("SELECT * FROM Discovery")
      .execute()
      .onSuccess(rows ->
      {

        System.out.println("Result "+rows.rowCount());

        List<Discovery> discoveries = new ArrayList<>();

        var response = new JsonArray();

        for (Row row : rows)
        {
          var discovery = new Discovery();

          discovery.setId(row.getInteger("id"));

          discovery.setName(row.getString("name"));

          discovery.setCredentialId(row.getInteger("credentialID"));

          discovery.setProvisioned(row.getBoolean("is_provisioned"));

          discovery.setIp(row.getString("ip"));

          discovery.setCreated_at(row.getLocalDateTime("created_at").toString());

          System.out.println("Discovery "+discovery);

          response.add(discovery.toJson());

        }

        context.response().putHeader("Content-Type", "application/json")
          .end(response.encodePrettily());

      })
      .onFailure(err ->
      {
        context.response().setStatusCode(500).end("Failed to fetch discoveries: " + err.getMessage());
      });
  }

  void updateDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name");

    var newName = context.request().getFormAttribute("username");

    var credentialName = context.request().getFormAttribute("credential_name");
    var isProvisioned = context.request().getFormAttribute("is_provisioned");

    // Find the credential ID first
    Promise<Integer> credentialIdPromise = Promise.promise();

    findCredentialId(credentialName, credentialIdPromise);

    credentialIdPromise.future().onComplete(ar ->
    {
      if (ar.succeeded())
      {
        int credentialId = ar.result();

        sqlClient
          .preparedQuery("UPDATE Discovery SET name = ?, credentialID = ?, is_provisioned = ? WHERE name = ?")
          .execute(Tuple.of(newName, credentialId, isProvisioned, name))
          .onSuccess(rows -> {
            if (rows.rowCount() == 0) {
              context.response().setStatusCode(404).end("Discovery not found.");
            } else {
              context.response().setStatusCode(200).end("Discovery updated successfully.");
            }
          })
          .onFailure(err -> {
            context.response().setStatusCode(500).end("Failed to update discovery: " + err.getMessage());
          });
      } else {
        context.response().setStatusCode(404).end(ar.cause().getMessage());
      }
    });
  }

  void deleteDiscovery(RoutingContext context) {
    var name = context.pathParam("name");

    sqlClient
      .preparedQuery("DELETE FROM Discovery WHERE name = ?")
      .execute(Tuple.of(name))
      .onSuccess(rows -> {
        if (rows.rowCount() == 0) {
          context.response().setStatusCode(404).end("Discovery not found.");
        } else {
          context.response().setStatusCode(200).end("Discovery deleted successfully.");
        }
      })
      .onFailure(err -> {
        context.response().setStatusCode(500).end("Failed to delete discovery: " + err.getMessage());
      });
  }

  void provisionDiscovery(RoutingContext context)
  {
    var name = context.pathParam("name").trim();

    System.out.println("Provision RouteHit");
    //if the device is already provisioned then return ..In polling
    //else returns polling started
    sqlClient.preparedQuery("UPDATE Discovery SET is_provisioned = TRUE WHERE name = ?")

      .execute(Tuple.of(name))

      .onComplete(result ->
      {
          if(result.succeeded())
          {

            context.response().end(result.result().toString());
          }
          else
            context.response().end("Discovery Not Found!");
      }).
      onFailure(failureHandler->{

        context.response().end("Discovery Not Found!");

      });


  }
}
