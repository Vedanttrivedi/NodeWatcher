package com.example.nodewatcher.routes;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.db.CredentialDB;
import com.example.nodewatcher.db.DiscoveryDB;
import com.example.nodewatcher.models.Credential;
import com.example.nodewatcher.service.PluginDataSaver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.sqlclient.SqlClient;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.AbstractCollection;

public class CredentialsRoutes extends AbstractVerticle
{

  private final CredentialDB credentialDB = new CredentialDB();

  private final Router router;

  private SqlClient sqlClient;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PluginDataSaver.class);

  public CredentialsRoutes(Router router)
  {
    this.router = router;

    this.sqlClient = BootStrap.getDatabaseClient();

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception
  {
    attach();

    startPromise.complete();

  }

  public void attach()
  {

    router.post("/credential/create")

      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(ctx -> createCredential(ctx, sqlClient));

    router.get("/credential/get/")
      .handler(TimeoutHandler.create(5000))
      .handler(ctx -> getAllCredential(ctx, sqlClient));


    router.get("/credential/get/:name")
      .handler(TimeoutHandler.create(5000))
      .handler(ctx -> getCredential(ctx, sqlClient));


    router.put("/credential/update/:name")

      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(ctx -> updateCredential(ctx, sqlClient));

    router.delete("/credential/delete/:name")
      .handler(TimeoutHandler.create(4000))
      .handler(ctx -> deleteCredential(ctx, sqlClient));

  }


  private void createCredential(RoutingContext context, SqlClient sqlClient)
  {

    var name = context.request().getFormAttribute("name");

    var username = context.request().getFormAttribute("username");

    var password = context.request().getFormAttribute("password");

    if(username==null || name==null || password==null)
    {
      context.response().end("name,username and password are required");
      return;
    }

    if (password.length() < 8)
    {
      context.response().end("Password Length Must be 8");
      return;
    }

    if (username.trim().length() <= 0 || name.trim().length() <= 0) {
      context.response().end("Username and Name must not be empty");
      return;
    }

    vertx.executeBlocking(future->{

      credentialDB.save(sqlClient,
        new Credential(name, username, password, LocalDateTime.now().toString(), 1))

        .onSuccess(success->{

          future.complete();

        })
        .onFailure(failure->{

          future.fail("Credentials already present ");
        });

    },futureRes->{

      if(futureRes.succeeded())
        context.response().end("Credentials Added");
      else
        context.response().end(futureRes.cause().toString());
    });

  }

  private void getCredential(RoutingContext context, SqlClient sqlClient)
  {
    var name = context.pathParam("name").trim();

    if (name.length() <= 0)
      context.response().end("Error");

    System.out.println("For Param " + name);

    vertx.executeBlocking(future->{

      credentialDB.getCredential(sqlClient, name)
        .onFailure(failureHandler -> {

          future.fail(failureHandler.getCause().toString());

        }).
        onSuccess(successHandler -> {

          future.complete(successHandler.encodePrettily());

        });

    },futureRes->{

      if(futureRes.failed())
        context.response().end(futureRes.cause().toString());
      else
        context.response().end(futureRes.result().toString());

    });
  }

  // Method to retrieve all credentials
  private void getAllCredential(RoutingContext context, SqlClient sqlClient)
  {
    vertx.executeBlocking(future->{

      credentialDB.getCredential(sqlClient)

        .onFailure(failureHandler ->
        {
          future.fail(failureHandler.getCause().toString());
        })
        .onSuccess(successHandler -> {
          future.complete(successHandler.encodePrettily());
        });

    },futureRes->{

      if(futureRes.failed())
        context.response().end("Something went wrong! " + futureRes.cause().toString());
      else
        context.response().end(futureRes.result().toString());

    });
  }

  // Method to update a credential by name
  private void updateCredential(RoutingContext context, SqlClient sqlClient) {

    var name = context.pathParam("name");

    var newName = context.request().getFormAttribute("name");

    var username = context.request().getFormAttribute("username");

    if (username.trim().isEmpty() || name.trim().isEmpty() || newName.trim().isEmpty())
      context.response().end("Username,Updated Name and Old Credential name are required");

    var password = context.request().getFormAttribute("password");

    if (password.length() < 8)
      context.response().end("password length must be 8 characters");

    credentialDB.updateCredential(sqlClient, name,
        new Credential(newName, username, password, LocalDateTime.now().toString(), 1))

      .onSuccess(successHandler -> {

        context.response().end("Success : Row Updated");

      })
      .onFailure(failureHandler -> {

        context.response().end("Error : " + failureHandler.getLocalizedMessage());

      });

  }

  private void deleteCredential(RoutingContext context, SqlClient sqlClient)
  {

    var name = context.pathParam("name");

    vertx.executeBlocking(future->{

      credentialDB.deleteCredential(sqlClient, name)

        .onSuccess(successHandler -> {

          System.out.println("Success Result : " + successHandler);

          future.complete("Credential Deleted ");
        })

        .onFailure(failureHandler -> {

          System.out.println("failure Result " + failureHandler.getMessage());

          future.fail("Credential not found!");

        });

    },futureRes->{

      if(futureRes.failed())
      {
        context.response().end(futureRes.cause().toString());
      }
      else
        context.response().end(futureRes.result().toString());
    });
  }

}
