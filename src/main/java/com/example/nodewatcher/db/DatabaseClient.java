package com.example.nodewatcher.db;

import com.example.nodewatcher.BootStrap;
import com.example.nodewatcher.utils.Config;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;

public class DatabaseClient
{

  private static MySQLPool sqlClient;

  public static MySQLPool getClient()
  {

    var connectOptions = new MySQLConnectOptions()
      .setPort(DBAuth.PORT)
      .setHost(DBAuth.HOST)
      .setDatabase(DBAuth.DATABASE)
      .setUser(DBAuth.USERNAME)
      .setPassword(DBAuth.PASSWORD);

    var poolOptions = new PoolOptions()
      .setMaxSize(Config.DB_POOL_SIZE);

    sqlClient = MySQLPool.pool(BootStrap.vertx, connectOptions, poolOptions);

    return sqlClient;

  }

}
