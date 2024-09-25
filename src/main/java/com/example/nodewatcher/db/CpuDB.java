package com.example.nodewatcher.db;

import com.example.nodewatcher.models.Cpu_Metric;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.sql.Timestamp;

public class CpuDB
{
  //this class does all memory dump operations

  public static void save(SqlClient sqlClient, Cpu_Metric cpuMetric, Timestamp timestamp)
  {

    System.out.println("In the save cpu "+cpuMetric.getIp()+"sql client :"+sqlClient);

    var discoveryIp = cpuMetric.getIp();

    sqlClient.preparedQuery("SELECT id FROM Discovery WHERE ip = ?")

      .execute(Tuple.of(discoveryIp))

      .onComplete(returnRowsResult -> {

        if(returnRowsResult.succeeded())
        {
          returnRowsResult.result().forEach(row->{

            var discovery_id = row.getInteger(0);

            System.out.println("Discovery Id "+ discovery_id);

            sqlClient.preparedQuery("INSERT INTO CPU_Metric(discoveryId,percentage,load_average,process_counts," +
                "io_percent,threads,created_at) VALUES (?,?,?,?,?,?,?)")

              .execute(Tuple.of(discovery_id,cpuMetric.getPercentage(),cpuMetric.getLoad_average(),
                cpuMetric.getProcess_counts(),cpuMetric.getIo_percent(),cpuMetric.getThreads(),timestamp))

              .onComplete(result ->{

                if(result.succeeded())
                {
                  System.out.println("Rows Added in CPU DB ");

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
