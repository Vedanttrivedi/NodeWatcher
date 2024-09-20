package com.example.nodewatcher.db;

import com.example.nodewatcher.models.Memory_Metric;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.sql.Timestamp;

public class MemoryDB
{
  //this class does all memory dump operations

  public void saveMemory(SqlClient sqlClient, Memory_Metric memoryMetric, Timestamp timestamp)
  {

    System.out.println("In the save memroyrrrrrrrrrrr "+memoryMetric.ip()+"sql client :"+sqlClient);


    var discoveryIp = memoryMetric.ip();


    sqlClient.preparedQuery("SELECT id FROM Discovery WHERE ip = ?")

      .execute(Tuple.of(discoveryIp))

      .onComplete(returnRowsResult -> {

        if(returnRowsResult.succeeded())
        {
          returnRowsResult.result().forEach(row->{

            var discovery_id = row.getInteger(0);

            System.out.println("Discovery Id "+ discovery_id);

            sqlClient.preparedQuery("INSERT INTO Memory_Metric(discoveryId,free,swap,used," +
              "cache,disc_used,created_at) VALUES (?,?,?,?,?,?,?)")

              .execute(Tuple.of(discovery_id,memoryMetric.free(),memoryMetric.swap(),
                memoryMetric.used(),memoryMetric.cached(),memoryMetric.disk_space(),timestamp))

              .onComplete(result ->{

                if(result.succeeded())
                {
                  System.out.println("Rows Added in db ");

                }
                else
                {
                  System.out.println("Failed while adding rows "+result.result());
                }

                }).onFailure(failure->{

                System.out.println("Failed last "+failure.getMessage());
              });
          });
        }
      });

  }
}
