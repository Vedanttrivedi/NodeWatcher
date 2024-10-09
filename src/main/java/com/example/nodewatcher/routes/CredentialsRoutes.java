package com.example.nodewatcher.routes;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.db.CredentialDB;
import com.example.nodewatcher.models.Credential;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import java.time.LocalDateTime;

public class CredentialsRoutes
{

  private final CredentialDB credentialDB;

  private final Router router;

  public CredentialsRoutes(Router router)
  {
    this.router = router;

    credentialDB = new CredentialDB(BootStrap.getDatabaseClient());
  }

  public void attach()
  {

    router.post("/credential/create")

      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(this::createCredential);

    router.get("/credential/get/")
      .handler(TimeoutHandler.create(5000))
      .handler(this::getAllCredential);


    router.get("/credential/get/:name")
      .handler(TimeoutHandler.create(5000))
      .handler(this::getCredential);


    router.put("/credential/update/:name")

      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(this::updateCredential);

    router.delete("/credential/delete/:name")
      .handler(TimeoutHandler.create(4000))
      .handler(this::deleteCredential);

  }


  private void createCredential(RoutingContext context)
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

    credentialDB.save(
      new Credential(name, username, password, LocalDateTime.now().toString(), 1))

      .onSuccess(success->{
        context.response().end("Credentials Added");

      })
      .onFailure(failure->{
        context.response().end("Credentials already present");

      });

  }

  private void getCredential(RoutingContext context)
  {
    var name = context.pathParam("name").trim();

    if (name.length() <= 0)
      context.response().end("Error");


    credentialDB.getCredential(name)
      .onFailure(failureHandler -> {

        context.response().end(failureHandler.getCause().toString());

      }).
      onSuccess(successHandler -> {
        context.response().end(successHandler.encodePrettily());

      });

  }

  private void getAllCredential(RoutingContext context)
  {
    credentialDB.getCredential()

        .onFailure(failureHandler ->
        {
          context.response().end(failureHandler.getCause().toString());
        })
        .onSuccess(successHandler -> {
          context.response().end(successHandler.encodePrettily());
        });

  }

  private void updateCredential(RoutingContext context) {

    var name = context.pathParam("name");

    var newName = context.request().getFormAttribute("name");

    var username = context.request().getFormAttribute("username");

    if (username.trim().isEmpty() || name.trim().isEmpty() || newName.trim().isEmpty())
      context.response().end("Username,Updated Name and Old Credential name are required");

    var password = context.request().getFormAttribute("password");

    if (password.length() < 8)
      context.response().end("password length must be 8 characters");

    credentialDB.updateCredential(name,
        new Credential(newName, username, password, LocalDateTime.now().toString(), 1))

      .onSuccess(successHandler -> {

        context.response().end("Success : Row Updated");

      })
      .onFailure(failureHandler -> {

        context.response().end("Error : " + failureHandler.getLocalizedMessage());

      });

  }

  private void deleteCredential(RoutingContext context)
  {

    var name = context.pathParam("name");

    credentialDB.deleteCredential(name)

    .onSuccess(successHandler -> {

        context.response().end("Credential Deleted ");
      })

    .onFailure(failureHandler -> {

        context.response().end("Credential not found!");

      });

  }

}
