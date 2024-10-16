package com.example.nodewatcher.web.routes;

import com.example.nodewatcher.Bootstrap;
import com.example.nodewatcher.database.Credential;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import java.time.LocalDateTime;

public class Credentials implements RouteServiceOperations
{

  private final Credential credentialDB;

  private final  Router router;

  public Credentials(Router router)
  {
    this.router = router;

    credentialDB = new Credential(Bootstrap.databaseClient);
  }

  public  void attach()
  {

    router.post("/create")

      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(this::create);

    router.get("/get/")
      .handler(TimeoutHandler.create(5000))
      .handler(this::getAll);


    router.get("/get/:name")
      .handler(TimeoutHandler.create(5000))
      .handler(this::get);


    router.put("/update/:name")

      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(this::update);

    router.delete("/delete/:name")
      .handler(TimeoutHandler.create(4000))
      .handler(this::delete);

  }


  public void create(RoutingContext context)
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
      new com.example.nodewatcher.models.Credential(name, username, password, LocalDateTime.now().toString(), 1))

      .onSuccess(success->{
        context.response().end("Credentials Added");

      })
      .onFailure(failure->{
        context.response().end("Credentials already present");

      });

  }

  public  void get(RoutingContext context)
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

  public  void getAll(RoutingContext context)
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

  public  void update(RoutingContext context) {

    var name = context.pathParam("name");

    var newName = context.request().getFormAttribute("name");

    var username = context.request().getFormAttribute("username");

    if (username.trim().isEmpty() || name.trim().isEmpty() || newName.trim().isEmpty())
      context.response().end("Username,Updated Name and Old Credential name are required");

    var password = context.request().getFormAttribute("password");

    if (password.length() < 8)
      context.response().end("password length must be 8 characters");

    credentialDB.updateCredential(name,
        new com.example.nodewatcher.models.Credential(newName, username, password, LocalDateTime.now().toString(), 1))

      .onSuccess(successHandler -> {

        context.response().end("Success : Row Updated");

      })
      .onFailure(failureHandler -> {

        context.response().end("Error : " + failureHandler.getLocalizedMessage());

      });

  }

  public  void delete(RoutingContext context)
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
