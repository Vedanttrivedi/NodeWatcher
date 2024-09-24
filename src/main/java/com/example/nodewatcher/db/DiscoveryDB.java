package com.example.nodewatcher.db;

import com.example.nodewatcher.models.Discovery;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public class DiscoveryDB
{

  private final SqlClient sqlClient;

  public DiscoveryDB(SqlClient sqlClient)
  {
    this.sqlClient = sqlClient;
  }

  public Future<Integer> findCredentialId(String credentialName)
  {
    Promise<Integer> promise = Promise.promise();
    sqlClient.preparedQuery("SELECT id FROM Credentials WHERE name = ?")
      .execute(Tuple.of(credentialName.trim()))
      .onSuccess(rows -> {
        if (rows.size() > 0)
        {
          var credentialId = rows.iterator().next().getInteger(0);
          promise.complete(credentialId);
        }
        else
        {
          promise.fail("Credentials do not exist");
        }
      })
      .onFailure(promise::fail);
    return promise.future();
  }

  public Future<Void> createDiscovery(String name, String ip, int credentialId) {
    return sqlClient.preparedQuery("INSERT INTO Discovery (name, ip, credentialID) VALUES (?, ?, ?)")
      .execute(Tuple.of(name, ip, credentialId))
      .mapEmpty();
  }

  public Future<RowSet<Row>> getDiscoveryAndCredentialByDiscoveryName(String discoveryName)
  {

    return sqlClient.preparedQuery("SELECT c.username,c.password,d.ip,d.name " +
      "FROM Discovery d JOIN Credentials c ON d.credentialID = c.id WHERE  d.name = ? ")
      .execute(Tuple.of(discoveryName));

  }


  public Future<Discovery> getDiscovery(String name)
  {
    return sqlClient.preparedQuery("SELECT * FROM Discovery WHERE name = ?")
      .execute(Tuple.of(name))
      .map(rows ->
      {
        if (rows.size() > 0)
        {
          var row = rows.iterator().next();

          return new Discovery(row.getInteger("id"), row.getString("name"), row.getInteger("credentialID"),row.getString("ip"),
            row.getBoolean("is_provisioned"), row.getLocalDateTime("created_at").toString());
        }

        return null;

      });
  }

  public Future<JsonArray> getAllDiscoveries()
  {
    return sqlClient.query("SELECT * FROM Discovery")
      .execute()
      .map(rows -> {

        var response = new JsonArray();

        rows.forEach(row ->
        {

          var discovery = new Discovery(row.getInteger("id"), row.getString("name"), row.getInteger("credentialID"),row.getString("ip"),
            row.getBoolean("is_provisioned"), row.getLocalDateTime("created_at").toString());

          response.add(discovery.toJson("Down"));
        });
        return response;
      });
  }

  public Future<Void> updateDiscovery(String name, String ip)
  {
    return sqlClient.preparedQuery("UPDATE Discovery SET ip = ? WHERE name = ?")
      .execute(Tuple.of(ip, name))
      .mapEmpty();
  }

  public Future<Void> deleteDiscovery(String name)
  {
    return sqlClient.preparedQuery("DELETE FROM Discovery WHERE name = ?")
      .execute(Tuple.of(name))
      .mapEmpty();
  }

  public Future<RowSet<Row>>provisionDiscovery(String name,boolean status)
  {
    if(status==false)
    {
      return sqlClient.preparedQuery("UPDATE Discovery SET is_provisioned = ? WHERE name = ? AND is_provisioned = ?")
        .execute(Tuple.of(status,name,1));
    }
    else
    {
      return sqlClient.preparedQuery("UPDATE Discovery SET is_provisioned = ? WHERE name = ? AND is_provisioned = ?")
        .execute(Tuple.of(status,name,0));
    }
  }

  public Future<Void> sameIpAndDiscoveryNameExists(String ip, String name, Promise<String> discoveryIpNamePromise)
  {

    return sqlClient.preparedQuery("SELECT * FROM Discovery WHERE name = ? OR ip = ?")
      .execute(Tuple.of(name, ip))
      .onSuccess(rows ->
      {
        if(rows.rowCount()==0)
          discoveryIpNamePromise.complete("Add");
        else
          discoveryIpNamePromise.complete("Remove");

      })
      .onFailure(failureHandler->{

        discoveryIpNamePromise.fail(failureHandler.getCause());

      }).mapEmpty();
  }
}
