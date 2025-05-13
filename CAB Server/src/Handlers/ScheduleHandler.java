package Handlers;
import static Utilities.ClerkAPI.getUser;
import static Utilities.FilterCourses.filterCourses;

import Utilities.ClerkAPI;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import spark.Request;
import spark.Response;
import spark.Route;

public class ScheduleHandler implements Route {
  public Object handle(Request req, Response res) {
    try {
      String userId = req.queryParams("user");
      JsonNode userData = ClerkAPI.getUser(userId);
      res.type("application/json");
      return userData.get("unsafe_metadata"); // Forward Clerk's response
    } catch (Exception e) {
      res.status(500);
      return "{\"error\":\"Backend failed to fetch user\"}";
    }
  }
}



