package com.example.nodewatcher.db;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

interface DBOperations
{
  Future<Void> save(SqlClient sqlClient);

  Future<JsonArray> get(SqlClient sqlClient);

  Future<JsonObject> get(SqlClient sqlClient, String name);

  Future<Boolean> delete(SqlClient sqlClient, String name);

}
