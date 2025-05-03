import static spark.Spark.after;
import static spark.Spark.options;

import Handlers.FilterHandler;
import Handlers.GenerateScheduleHandler;
import spark.Spark;

/**
 * The Main class of our project. This is where execution begins.
 */
public final class Server {

  /**
   * The main method is the entry point of the application.
   *
   * @param args command-line arguments passed to the program
   */
  public static void main(String[] args) {
    int port = 3232;
    Spark.port(port);

    after((request, response) -> {
      response.header("Access-Control-Allow-Origin", "*");
      response.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      response.header("Access-Control-Allow-Headers", "Content-Type");
    });

    // Handle OPTIONS requests
    options("/*", (request, response) -> {
      return "OK";
    });

    // Course filtering endpoint
    Spark.get("/filter", new FilterHandler());

    // Schedule generation endpoint
    Spark.post("/generate", new GenerateScheduleHandler());

    Spark.init();
    Spark.awaitInitialization();
    System.out.println("Server started at http://localhost:" + port);
  }
}