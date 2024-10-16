package com.example.nodewatcher.database;

import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class Credential
{

  private final SqlClient sqlClient;

  public Credential(SqlClient sqlClient)
  {
    this.sqlClient = sqlClient;
  }


  public Future<Void> save(com.example.nodewatcher.models.Credential credential)
  {
    var query = "INSERT INTO Credentials (name, username, password, protocol) VALUES (?, ?, ?, ?)";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(credential.name(), credential.username(), credential.password() , credential.protocol()))
      .mapEmpty();

  }

  public Future<JsonObject> getCredential( String name)
  {
    var query = "SELECT * FROM Credentials WHERE name = ?";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(name))
      .map(resultSet -> {

          var row = resultSet.iterator().next();

          var credential = new JsonObject()
          .put("name", row.getString("name"))
          .put("username", row.getString("username"))
          .put("protocol", row.getInteger("protocol"))
          .put("created_at", row.getLocalDateTime("created_at").toString());

          return credential;

      });
  }

  public Future<JsonArray> getCredential()
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
  public Future<Boolean> deleteCredential(String name)
  {
    var query = "DELETE FROM Credentials WHERE name = ?";

    return sqlClient.preparedQuery(query)
      .execute(Tuple.of(name))

      .map(result -> result.rowCount() > 0);

  }

  public Future<Boolean> updateCredential(String name, com.example.nodewatcher.models.Credential credential)
  {
    String query = "UPDATE Credentials SET name = ?, username = ?, password = ?, protocol = ? WHERE name = ?";

    return sqlClient
      .preparedQuery(query)

      .execute(Tuple.of(credential.name(), credential.username(), credential.password(), credential.protocol(), name))

      .map(result -> result.rowCount() > 0);
  }

}

