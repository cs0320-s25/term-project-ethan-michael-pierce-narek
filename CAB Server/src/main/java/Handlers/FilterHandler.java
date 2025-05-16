package Handlers;

import static Utilities.FilterCourses.filterCourses;

import java.io.IOException;
import spark.Request;
import spark.Response;
import spark.Route;

public class FilterHandler implements Route {
  public Object handle(Request request, Response response) throws Exception {
    try {
      String term = request.queryParams("term");
      if (term == null) {
        throw new IOException("Please input a term code");
      }
      String dept = request.queryParams("dept");
      String time = request.queryParams("time");
      String day = request.queryParams("day");
      Boolean writ = Boolean.valueOf(request.queryParams("writ"));

      return filterCourses(term, dept, time, day, writ);

    } catch (Exception e) {

      return "error" + e.getMessage();
    }
  }
}
