package com.example.nodewatcher.db;

import com.example.nodewatcher.models.Credential;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class CredentialDB
{

  public Future<Void> save(SqlClient sqlClient, Credential credential)
  {
    String query = "INSERT INTO Credentials (name, username, password, protocol) VALUES (?, ?, ?, ?)";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(credential.name(), credential.username(), credential.password(), credential.protocol()))
      .mapEmpty();  // Return a Future that completes when operation succeeds

  }

  public Future<JsonObject> getCredential(SqlClient sqlClient, String name)
  {
    String query = "SELECT * FROM Credentials WHERE name = ?";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(name))
      .map(resultSet -> {

        Row row = resultSet.iterator().next();
        JsonObject credential = new JsonObject()
          .put("name", row.getString("name"))
          .put("username", row.getString("username"))
          .put("protocol", row.getInteger("protocol"))
          .put("created_at", row.getLocalDateTime("created_at").toString());
        return credential;
      });
  }

  public Future<JsonArray> getCredential(SqlClient sqlClient)
  {
    var query = "SELECT * FROM Credentials";

    return sqlClient.query(query)
      .execute()

      .map(resultSet ->
      {
        var credentialsArray = new JsonArray();

        resultSet.forEach(row ->
        {
          var credential = new JsonObject()
            .put("name", row.getString("name"))
            .put("username", row.getString("username"))
            .put("protocol", row.getInteger("protocol"))
            .put("created_at", row.getLocalDateTime("created_at").toString());

          credentialsArray.add(credential);

        });

        return credentialsArray;

      });
  }


  // Delete a credential by ID
  public Future<Boolean> deleteCredential(SqlClient sqlClient, String name)
  {
    var query = "DELETE FROM Credentials WHERE name = ?";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(name))

      .map(result -> result.rowCount() > 0);  // Return true if deleted, false otherwise

  }

  // Update a credential by ID
  public Future<Void> updateCredential(SqlClient sqlClient, String name,Credential credential) {
    String query = "UPDATE Credentials SET name = ?, username = ?, password = ?, protocol = ? WHERE name = ?";

    return sqlClient
      .preparedQuery(query)

      .execute(Tuple.of(credential.name(), credential.username(), credential.password(), credential.protocol(), name))

      .flatMap(result -> {
        if (result.rowCount() > 0)
        {
          return Future.succeededFuture();
        }
        else
        {
          return Future.failedFuture("No rows were updated, credential not found");
        }
      });
  }

}

