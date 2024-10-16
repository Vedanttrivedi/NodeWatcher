package com.example.nodewatcher.db;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.models.Credential;
import com.example.nodewatcher.utils.Address;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.file.AccessDeniedException;
import java.util.Base64;

public class CredentialDB
{

  private final SqlClient sqlClient;

  public CredentialDB(SqlClient sqlClient)
  {
    this.sqlClient = sqlClient;
  }


  public Future<Void> save(Credential credential)
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

  public Future<Boolean> updateCredential(String name,Credential credential)
  {
    String query = "UPDATE Credentials SET name = ?, username = ?, password = ?, protocol = ? WHERE name = ?";

    return sqlClient
      .preparedQuery(query)

      .execute(Tuple.of(credential.name(), credential.username(), credential.password(), credential.protocol(), name))

      .map(result -> result.rowCount() > 0);
  }

}

