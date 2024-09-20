package com.example.nodewatcher.db;

import com.example.nodewatcher.models.Discovery;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryDB
{

  public Future<Integer> findCredentialId(SqlClient sqlClient, String credentialName)
  {
    Promise<Integer> promise = Promise.promise();
    sqlClient.preparedQuery("SELECT id FROM Credentials WHERE name = ?")
      .execute(Tuple.of(credentialName.trim()))
      .onSuccess(rows -> {
        if (rows.rowCount() > 0) {
          promise.complete(rows.iterator().next().getInteger("id"));
        } else {
          promise.complete(-1);
        }
      })
      .onFailure(err -> promise.fail("Failed to fetch credential: " + err.getMessage()));
    return promise.future();
  }

  public Future<Void> createDiscovery(SqlClient sqlClient, String name, String ip, int credentialId) {
    return sqlClient
      .preparedQuery("INSERT INTO Discovery (name, ip, credentialID) VALUES (?, ?, ?)")
      .execute(Tuple.of(name, ip, credentialId))
      .mapEmpty();
  }

  public Future<JsonObject> getDiscovery(SqlClient sqlClient, String name) {
    return sqlClient.preparedQuery("SELECT * FROM Discovery WHERE name = ?")
      .execute(Tuple.of(name))
      .map(rows -> {
        if (rows.rowCount() > 0) {
          var row = rows.iterator().next();
          return new JsonObject()
            .put("id", row.getInteger("id"))
            .put("name", row.getString("name"))
            .put("credentialID", row.getInteger("credentialID"))
            .put("is_provisioned", row.getBoolean("is_provisioned"))
            .put("created_at", row.getString("created_at"));
        } else {
          return null;
        }
      });
  }

  public Future<List<Discovery>> getAllDiscovery(SqlClient sqlClient) {
    return sqlClient.query("SELECT * FROM Discovery")
      .execute()
      .map(rows -> {
        List<Discovery> discoveries = new ArrayList<>();
        rows.forEach(row -> {
          Discovery discovery = new Discovery();
          discovery.setId(row.getInteger("id"));
          discovery.setName(row.getString("name"));
          discovery.setCredentialId(row.getInteger("credentialID"));
          discovery.setProvisioned(row.getBoolean("is_provisioned"));
          discovery.setIp(row.getString("ip"));
          discovery.setCreated_at(row.getString("created_at"));
          discoveries.add(discovery);
        });
        return discoveries;
      });
  }

  public Future<Boolean> deleteDiscovery(SqlClient sqlClient, String name) {
    return sqlClient.preparedQuery("DELETE FROM Discovery WHERE name = ?")
      .execute(Tuple.of(name))
      .map(result -> result.rowCount() > 0);
  }

  public Future<Void> updateDiscovery(SqlClient sqlClient, String name, String newName, int credentialId) {
    return sqlClient
      .preparedQuery("UPDATE Discovery SET name = ?, credentialID = ? WHERE name = ?")
      .execute(Tuple.of(newName, credentialId, name))
      .mapEmpty();
  }
}
