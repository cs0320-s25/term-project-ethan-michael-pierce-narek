package Handlers;

import static Utilities.FilterCourses.filterCourses;

import java.io.IOException;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handler for filtering courses based on specified criteria. This class implements the Spark Route
 * interface to handle HTTP requests for filtering courses in the Brown University course catalog.
 */
public class FilterHandler implements Route {

  /**
   * Handles HTTP requests to filter courses based on query parameters.
   *
   * @param request The HTTP request containing query parameters for filtering
   * @param response The HTTP response (not modified by this method)
   * @return A JSON string containing filtered course results or an error message
   * @throws Exception If an error occurs during filtering
   */
  @Override
  public Object handle(Request request, Response response) throws Exception {
    try {
      // Extract term parameter (required)
      String term = request.queryParams("term");
      if (term == null) {
        throw new IOException("Please input a term code");
      }

      // Extract optional filter parameters
      String dept = request.queryParams("dept"); // Department code filter
      String time = request.queryParams("time"); // Class time filter
      String day = request.queryParams("day"); // Class day filter
      Boolean writ = Boolean.valueOf(request.queryParams("writ")); // WRIT designation filter

      // Call the filterCourses utility method to perform the actual filtering
      return filterCourses(term, dept, time, day, writ);
    } catch (Exception e) {
      // Return error message if any exception occurs
      return "error" + e.getMessage();
    }
  }
}
