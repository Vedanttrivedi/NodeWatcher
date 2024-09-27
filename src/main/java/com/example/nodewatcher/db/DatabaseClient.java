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

    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setPort(DBAuth.DBPORT)
      .setHost(DBAuth.DBHOST)
      .setDatabase(DBAuth.DBNAME)
      .setUser(DBAuth.DBUSER)
      .setPassword(DBAuth.DBPASSWORD);

    System.out.println("Database Connection Established for user: " + connectOptions.getUser());

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5);

    sqlClient = MySQLPool.pool(vertx, connectOptions, poolOptions);

    return sqlClient;

  }
}
