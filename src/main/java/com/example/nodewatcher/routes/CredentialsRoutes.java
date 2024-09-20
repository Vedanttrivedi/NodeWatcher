package com.example.nodewatcher.routes;

import com.example.nodewatcher.db.CredentialDB;
import com.example.nodewatcher.models.Credential;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDateTime;

public class CredentialsRoutes
{
  static CredentialDB credentialDB = new CredentialDB();

  public static void attach(Router router, SqlClient sqlClient)
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


  static void createCredential(RoutingContext context, SqlClient sqlClient)
  {

    var name = context.request().getFormAttribute("name");

    var username = context.request().getFormAttribute("username");

    var password = context.request().getFormAttribute("password");

    if(password.length() < 8)
    {
      context.response().end("Password Length Must be 8");
      return;
    }

    if(username.trim().length()  <= 0 || name.trim().length() <= 0 )
    {
      context.response().end("Username and Name must not be empty");
      return;
    }
//    var protocol = context.request().getFormAttribute("protocol");
      var protocol = 1;//1 for ssh , 2 for winrm

    Future<Void> result = credentialDB.save(sqlClient,
      new Credential(name,username,password, LocalDateTime.now().toString(),1));

    result.onSuccess(queryResult->{

        context.response().end("<h1 Credentials added</h1>");

    }).
      onFailure(queryFailure->
      {

        if(result.cause().getLocalizedMessage().contains("Duplicate"))
          context.response().end("<h1> Duplicate Entry For Credential Name </h1>");

        else
          context.response().end("<h1> "+result.cause().getLocalizedMessage()+"</h1>");

      });

  }

  // Method to retrieve a specific credential by name
  static void getCredential(RoutingContext context, SqlClient sqlClient)
  {
    var name = context.pathParam("name").trim();

    if(name.length() <= 0)
      context.response().end("Error");

    System.out.println("For Param "+name);

    Future<JsonObject>getResult = credentialDB.getCredential(sqlClient,name);

    getResult.onFailure(failureHandler->{

      context.response().end("Credentials Not Found "+failureHandler.getMessage());

    }).
      onSuccess(successHandler->{

        context.response().end(successHandler.encodePrettily());

    });

  }

  // Method to retrieve all credentials
  static void getAllCredential(RoutingContext context, SqlClient sqlClient)
  {

    System.out.println("Need All Credentials");

    credentialDB.getCredential(sqlClient)

      .onFailure(failureHandler->{

        context.response().end("Something went wrong! "+failureHandler.getLocalizedMessage());

      })
      .onSuccess(successHandler->{

        context.response().end(successHandler.encodePrettily());

      });
  }

  // Method to update a credential by name
  static void updateCredential(RoutingContext context, SqlClient sqlClient)
  {

    var name = context.pathParam("name");

    var newName = context.request().getFormAttribute("name");

    var username = context.request().getFormAttribute("username");

    if(username.trim().isEmpty() || name.trim().isEmpty()|| newName.trim().isEmpty())
      context.response().end("Username,Updated Name and Old Credential name are required");

    var password = context.request().getFormAttribute("password");

    if(password.length() < 8 )
      context.response().end("password length must be 8 characters");

    var protocol = 1;

   credentialDB.updateCredential(sqlClient,name,
     new Credential(newName,username,password,LocalDateTime.now().toString(),1))

     .onSuccess(successHandler->{

       context.response().end("Success : Row Updated");

     })
     .onFailure(failureHandler->{

       context.response().end("Error : "+failureHandler.getLocalizedMessage());

     });

  }

  static void deleteCredential(RoutingContext context, SqlClient sqlClient)
  {

    var name = context.pathParam("name");

    credentialDB.deleteCredential(sqlClient,name)

      .onSuccess(successHandler->{

        System.out.println("Success Result : "+successHandler);

      })

      .onFailure(failureHandler->{

        System.out.println("failure Result "+failureHandler.getMessage());

      });
  }
}
