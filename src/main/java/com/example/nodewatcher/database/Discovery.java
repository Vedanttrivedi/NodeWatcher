package com.example.nodewatcher.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public class Discovery
{

  private final SqlClient sqlClient;

  public Discovery(SqlClient sqlClient)
  {
    this.sqlClient = sqlClient;
  }



  public Future<RowSet<Row>> getDiscoveryAndCredentialByDiscoveryName(String discoveryName)
  {

    return sqlClient.preparedQuery("SELECT c.username,c.password,d.ip,d.name " +
      "FROM Discovery d JOIN Credentials c ON d.credentialID = c.id WHERE  d.name = ? ")
      .execute(Tuple.of(discoveryName));

  }


  public Future<JsonObject> getDiscovery(String name)
  {

    return sqlClient.preparedQuery("select d.name,d.ip,c.username,c.password,d.is_provisioned from Discovery d " +
        "join Credentials c ON c.id = d.credentialID where d.name = ?")
      .execute(Tuple.of(name))
      .map(rows ->
      {
        if (rows.size() > 0)
        {
          var row = rows.iterator().next();

          var data = new JsonObject();

          data.put("name",row.getString("name"));

          data.put("ip",row.getString("ip"));

          data.put("username",row.getString("username"));


          data.put("password",row.getString("password"));

          data.put("is_provision",row.getBoolean("is_provisioned"));

          return data;
        }

        return null;

      });
  }


  public Future<JsonArray> getAllDiscoveries()
  {
    return sqlClient.query("SELECT d.name,d.ip,c.username FROM Discovery d " +
        "JOIN Credentials c ON d.credentialID = c.id")
      .execute()
      .map(rows -> {

        var response = new JsonArray();

        rows.forEach(row ->
        {
          var data = new JsonObject();

          data.put("name",row.getString(0));

          data.put("ip",row.getString(1));

          data.put("username",row.getString(2));


          response.add(data);

        });
        return response;
      });
  }

  public Future<Integer> updateDiscovery(String name, String ip)
  {
    return sqlClient.preparedQuery("UPDATE Discovery SET ip = ? WHERE name = ? ")
      .execute(Tuple.of(ip, name))
      .map(rows -> rows.rowCount());
  }

  public Future<Integer> updateDiscovery(String name, String ip, String credential_name)
  {
    Promise<Integer> promise = Promise.promise();

    sqlClient.preparedQuery("SELECT id FROM Credentials WHERE name = ? ")
      .execute(Tuple.of(credential_name))
      .onComplete(

        result->{

          if(result.result().size()==1)
          {
            var id = result.result().iterator().next().getInteger(0);

            sqlClient.preparedQuery("UPDATE Discovery SET ip = ? , credentialId = ? where name = ?")
              .execute(Tuple.of(ip,id,name))
              .onComplete(updateResult->
              {
                System.out.println("update result "+updateResult.result());

                if(updateResult.succeeded())
                  promise.complete(updateResult.result().rowCount());
                else
                  promise.fail("Could not update DB error");
              });
          }
          else
          {
            promise.fail("credential does not exists");}

      });

    return promise.future();
  }

  public Future<String> deleteDiscovery(String name)
  {
    Promise<String> promise = Promise.promise();

  sqlClient.preparedQuery("SELECT name,is_provisioned FROM Discovery WHERE name = ?")
      .execute(Tuple.of(name))
      .onComplete(result->{

        if(result.result().size()!=0)
          {
            System.out.println("Row Information "+result.result());
            var row = result.result().iterator().next();
            System.out.println("row info "+row.getString(0));
            System.out.println("row 2 "+row.getBoolean(1));
            System.out.println("Row Information "+row);
            if(row.getBoolean(1))
            {
              promise.complete("Discovery is in provision state . You cannot delete. You must first unprovision it");

            }
            else
            {
              sqlClient.preparedQuery("DELETE FROM Discovery where name = ? ")
                .execute(Tuple.of(name))
                .onComplete(deleteRes->{
                  if(deleteRes.succeeded())
                    promise.complete("Discovery Deleted");
                  else
                    promise.fail(deleteRes.cause().getMessage());
                });
            }

          }
          else
          {
            promise.fail("Discovery does not exists");
          }
      });

    return promise.future();
  }

  public Future<RowSet<Row>>provisionDiscovery(String name,boolean status)
  {
    if(!status)
    {
      return sqlClient.preparedQuery("UPDATE Discovery SET is_provisioned = ? WHERE name = ? AND is_provisioned = ?")
        .execute(Tuple.of(false,name,1));
    }
    else
    {
      return sqlClient.preparedQuery("UPDATE Discovery SET is_provisioned = ? WHERE name = ? AND is_provisioned = ?")
        .execute(Tuple.of(true,name,0));
    }
  }

  public Future<String> sameIpAndDiscoveryNameExists(String ip, String name)
  {
    return sqlClient.preparedQuery("SELECT 1 FROM Discovery WHERE ip = ? OR name = ?")
      .execute(Tuple.of(ip, name))
      .map(rows -> rows.size() > 0 ? "Present" : "Not Present");
  }

  public Future<Void> createDiscovery(String name, String ip, int credentialId)
  {
    return sqlClient.preparedQuery("INSERT INTO Discovery (name, ip, credentialID) VALUES (?, ?, ?)")
      .execute(Tuple.of(name, ip, credentialId))
      .mapEmpty();  // No return value required on success
  }

  public Future<JsonObject> findCredential(String credentialName)
  {
    Promise<JsonObject> promise = Promise.promise();

    sqlClient.preparedQuery("SELECT id,username,password FROM Credentials WHERE name = ?")
      .execute(Tuple.of(credentialName.trim()))
      .onSuccess(rows -> {
        if (rows.size() > 0)
        {

          var credentialId = rows.iterator().next().getInteger(0);

          var username = rows.iterator().next().getString(1);

          var password = rows.iterator().next().getString(2);


          var payLoad = new JsonObject();

          payLoad.put("id",credentialId);

          payLoad.put("username",username);

          payLoad.put("password",password);

          promise.complete(payLoad);
        }
        else
        {
          promise.fail("Credentials do not exist");
        }
      })
      .onFailure(promise::fail);

    return promise.future();
  }
}
