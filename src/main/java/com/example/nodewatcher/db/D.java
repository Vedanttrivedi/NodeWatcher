package taskCollector.db;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class D {
  public static void main(String[] args) {
    var vertx = Vertx.vertx();

    // MongoDB configuration
    JsonObject mongoConfig = new JsonObject()
      .put("connection_string", "mongodb://localhost:27017")
      .put("db_name", "TaskCollector");

    // Create MongoClient
    MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfig);

    // Data to insert
    JsonObject data = new JsonObject()
      .put("title", "Walk")
      .put("description", "Walking is good for health");

    // Attempt to save data to the "Todos" collection
    mongoClient.save("Todos", data, saveResult -> {
      if (saveResult.succeeded()) {
        System.out.println("Data saved with ID: " + saveResult.result());
      } else {
        System.out.println("Failed to save data: " + saveResult.cause());
      }

      // Ensure the program doesn't exit before the operation completes
      vertx.close();
    });

    // Keep the application running until Vert.x explicitly closes it
    vertx.deployVerticle(new io.vertx.core.AbstractVerticle() {
      @Override
      public void start() {
        // Do nothing, just keep Vert.x event loop alive
      }
    });
  }
}
