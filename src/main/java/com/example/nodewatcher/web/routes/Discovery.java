package com.example.nodewatcher.web.routes;

import com.example.nodewatcher.Bootstrap;
import com.example.nodewatcher.utils.Address;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.slf4j.LoggerFactory;

public class Discovery implements RouteServiceOperations
{

  private final com.example.nodewatcher.database.Discovery discoveryDB = new com.example.nodewatcher.database.Discovery(Bootstrap.databaseClient);

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Discovery.class);

  public void attach(Router router)
  {

    router.post("/create")
      .handler(TimeoutHandler.create(5000))
      .handler(BodyHandler.create())
      .handler(this::create);

    router.get("/get/:name").handler(this::get);

    router.get("/all").handler(this::getAll);

    router.put("/update/:name").
      handler(BodyHandler.create())
      .handler(this::update);

    router.delete("/delete/:name").handler(this::delete);

    router.patch("/provision/:name").
      handler(BodyHandler.create()).
      handler(this::provision);

  }

  public void get(RoutingContext context)
  {

    var name = context.pathParam("name").trim();

    if (name.isEmpty())
    {
      context.response().setStatusCode(400).end("Discovery name cannot be empty.");

      return;
    }

    discoveryDB.getDiscovery(name)
      .onComplete(result ->
      {
        if (result.succeeded())
        {

          var discovery = result.result();

          if (discovery == null)
            context.response().setStatusCode(404).end("Discovery does not exist.");

          else
          {

            Bootstrap.vertx.executeBlocking(sshFuture ->
            {

              sshFuture.complete(Config.ssh(discovery.getString("username"),
                discovery.getString("password"), discovery.getString("ip")));

            }, sshFutureRes ->
            {

              if (sshFutureRes.succeeded())
              {
                System.out.println("Result Of SSh Future " + sshFutureRes.result());

                if ((boolean) sshFutureRes.result())
                {
                  discovery.remove("passsword");
                  discovery.put("status", "Up");
                }
                else
                {
                  discovery.remove("passsword");
                  discovery.put("status", "Down");
                }
                context.response()
                  .putHeader("Content-Type", "application/json")
                  .end(discovery.encodePrettily());
              }

            });

          }
        }
        else
        {
          context.response().setStatusCode(500).end("Database error: " + result.cause().getMessage());
        }
      });
  }

  public void getAll(RoutingContext context)
  {

    discoveryDB.getAllDiscoveries()
      .onComplete(result ->
      {

        if (result.succeeded())
          context.response().end(result.result().encodePrettily());

        else
          context.response().end(result.cause().toString());

      });
  }

  public void update(RoutingContext context)
  {
    var name = context.pathParam("name");

    var ip = context.request().getFormAttribute("ip");

    if (context.request().getFormAttribute("credential_name") != null)
    {

      discoveryDB.updateDiscovery(name, ip, context.request().getFormAttribute("credential_name"))
        .onComplete(result ->
        {

          if (result.succeeded())
            context.response().end("Ip and Credential Updated For Discovery " + name);
          else
            context.response().end("Credential_name or Discovery name does not exists");
        });
    }
    else
    {
      discoveryDB.updateDiscovery(name, ip)
        .onComplete(result ->
        {

          if (result.result() == 1)
            context.response().end("Ip updated!");
          else
            context.response().end("Credential_name or Discovery name does not exists");

        });
    }

  }

  public void delete(RoutingContext context)
  {
    var name = context.pathParam("name");

    discoveryDB.deleteDiscovery(name)
      .onComplete(result ->
      {

        if (result.succeeded())
          context.response().end(result.result());

        else
          context.response().end(result.cause().getMessage());

      });

  }

  public void provision(RoutingContext context)
  {
    var name = context.pathParam("name").trim();


    var status = context.request().getFormAttribute("status");

    if (status == null)
    {
      context.response().end("Provision Status Missing. Provide status = 1 for provision and 0 to remove provision");
      return;
    }
    try
    {
      var doProvision = Integer.valueOf(status) == 1;
      {

        discoveryDB.provisionDiscovery(name, doProvision)

          .onSuccess(success ->
          {

            if (success.rowCount() != 0)
            {
              context.response().end("Discovery Provision Status Updated ");

              if (doProvision)
                logger.info("Polling started for  " + name);
              else
                logger.info("Polling stopped for  " + name);

              var discoveryAndCredentialBody = new JsonObject();

              discoveryDB.getDiscoveryAndCredentialByDiscoveryName(name)

                .onSuccess(

                  rowResult ->
                  {

                    discoveryAndCredentialBody.put("username", rowResult.iterator().next().getString(0));

                    discoveryAndCredentialBody.put("password", rowResult.iterator().next().getString(1));

                    discoveryAndCredentialBody.put("ip", rowResult.iterator().next().getString(2));

                    discoveryAndCredentialBody.put("name", rowResult.iterator().next().getString(3));

                    discoveryAndCredentialBody.put("doPolling", doProvision);

                    var tempArray = new JsonArray();

                    tempArray.add(discoveryAndCredentialBody);

                    tempArray.add(false);

                    Bootstrap.vertx.eventBus().send(Address.PLUGIN_DATA_SENDER, discoveryAndCredentialBody);

                  });


            }
            else
            {
              if (doProvision)
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

      logger.error("Error " + exception.getMessage());
    }

  }

  public void create(RoutingContext context)
  {
    try
    {

      var ip = context.request().getFormAttribute("ip");

      if (!Config.validIp(ip))
      {
        context.response().end("Invalid Ip");

        return;

      }

      var name = context.request().getFormAttribute("name");

      var credentialName = context.request().getFormAttribute("credential_name");

      discoveryDB.findCredential(credentialName)

        .compose(credential ->
        {

          if (credential == null)
            return Future.failedFuture("Credential not found");


          credential.put("ip", ip);

          Promise<JsonObject> dataPromise  =Promise.promise();

          Bootstrap.vertx.executeBlocking(sshPromise ->
          {

            sshPromise.complete(

              Config.ssh(credential.getString("username"), credential.getString("password"), ip));

          }, false, sshFutureResult ->
          {

            System.out.println("SSh Future Result " + sshFutureResult.result());

            var message = new JsonObject();

            message.put("credentials",credential);

            if ((boolean) sshFutureResult.result())
            {
              message.put("credentials",credential);

              message.put("sshReply","Discovered");

              dataPromise.complete(message);
            }

            else
            {
              message.put("sshReply","Not Discovered!");

              dataPromise.complete(message);
            }
          });

          return dataPromise.future();

        })
        .compose(result ->
        {

          var credential = result.getJsonObject("credential");

          var sshReply = result.getString("sshReply");

          return discoveryDB.sameIpAndDiscoveryNameExists(ip, name)

            .compose(duplicateRow ->
            {

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
      System.out.println("Error in create discovery " + exception.getMessage());
    }
  }

}
