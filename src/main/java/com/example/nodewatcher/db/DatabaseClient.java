package com.example.nodewatcher.db;

import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;

public class DatabaseClient
{
  private static MySQLPool sqlClient;

  public static MySQLPool getClient(Vertx vertx)
  {

    var connectOptions = new MySQLConnectOptions()
      .setPort(DBAuth.PORT)
      .setHost(DBAuth.HOST)
      .setDatabase(DBAuth.DATABASE)
      .setUser(DBAuth.USERNAME)
      .setPassword(DBAuth.PASSWORD);

    var poolOptions = new PoolOptions()
      .setMaxSize(5);

    sqlClient = MySQLPool.pool(vertx, connectOptions, poolOptions);

    return sqlClient;

  }
}
