package Handlers;

import Scheduler.ScheduleGenerator;
import static Utilities.ClerkAPI.getUser;
import com.fasterxml.jackson.databind.JsonNode;
import Scheduler.ScheduleGenerator.Result;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.util.*;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * HTTP handler for schedule generation requests.
 * This class processes requests to generate optimal course schedules based on
 * user preferences and constraints. It integrates with Clerk authentication
 * to retrieve user-specific metadata when available.
 */
public class ScheduleHandler implements Route {

  /** Moshi instance for JSON serialization and deserialization */
  private static final Moshi MOSHI = new Moshi.Builder().build();

  /** JSON adapter for converting between Maps and JSON strings */
  private static final JsonAdapter<Map<String, Object>> JSON =
      MOSHI.adapter(Types.newParameterizedType(Map.class, String.class, Object.class));

  /**
   * Handles HTTP requests for schedule generation.
   * This method processes the incoming request, validates parameters,
   * generates schedules, and returns the results as JSON.
   *
   * @param req The HTTP request containing schedule generation parameters
   * @param res The HTTP response object for setting status codes
   * @return A JSON string containing generated schedules or error information
   * @throws Exception If an error occurs during processing
   */
  @Override
  public Object handle(Request req, Response res) throws Exception {
    JsonNode meta = null;
    String userId = req.queryParams("user");

    // Retrieve user metadata from Clerk if a user ID is provided
    if (userId != null && !userId.isBlank()) {
      try {
        meta = getUser(userId).get("unsafe_metadata");
      } catch (Exception e) {
        res.status(400);
        return JSON.toJson(Map.of(
            "success", false,
            "errors", List.of("Invalid Clerk user ID: " + userId)
        ));
      }
    }

    List<String> errors = new ArrayList<>();

    // Parse and validate request parameters
    ScheduleErrorChecker.Params p = ScheduleErrorChecker.parseParams(req, meta, errors);

    if (!errors.isEmpty()) {
      return JSON.toJson(Map.of("success", false, "errors", errors));
    }

    // Pre-filter validation
    ScheduleErrorChecker.validatePreFilter(p, errors);
    if (!errors.isEmpty()) {
      return JSON.toJson(Map.of("success", false, "errors", errors));
    }

    // Initialize the schedule generator with validated parameters
    ScheduleGenerator gen =
        new ScheduleGenerator(
            p.classesPerSemester,
            p.coursesTaken,
            p.remainingRequired,
            p.necessaryCourses,
            p.availableTimes,
            p.dayAvailability,
            p.balance,
            p.requiredThisSem,
            p.preferredDepts,
            p.needWRIT);

    // Load course data for the requested term
    gen.loadCourseData(p.term);

    // Validate course existence and filter courses
    ScheduleErrorChecker.validateCourseExistence(p, gen.courseMap.keySet(), errors);
    gen.filterCourses(p.term);

    // Post-filter validation
    ScheduleErrorChecker.validatePostFilter(gen, p, errors);
    if (!errors.isEmpty()) {
      return JSON.toJson(Map.of("success", false, "errors", errors));
    }

    // Check WRIT requirement can be satisfied
    if (p.needWRIT
        && gen.getFilteredCourses().stream().noneMatch(c -> Boolean.TRUE.equals(c.get("writ")))) {
      return JSON.toJson(
          Map.of(
              "success",
              false,
              "errors",
              List.of("No WRIT‑designated course fits the given constraints")));
    }

    // Generate schedules and build the response
    Result result = gen.generateSchedules(p.term);
    return JSON.toJson(buildResponse(result));
  }

  /**
   * Builds a structured response object from the schedule generation results.
   * This method formats the schedule information into a JSON-compatible map structure.
   *
   * @param result The schedule generation result containing schedules and any errors
   * @return A map containing the formatted response with schedules and metadata
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> buildResponse(Result result) {
    Map<String, Object> outPut = new LinkedHashMap<>();
    outPut.put("success", result.errors.isEmpty());
    outPut.put("errors", result.errors);
    outPut.put("schedulesCount", result.schedules.size());

    List<Map<String, Object>> schedOut = new ArrayList<>();
    for (var schedule : result.schedules) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("score", schedule.score);

      // Extract essential course information for each schedule
      List<Map<String, Object>> cm = new ArrayList<>();
      for (var c : schedule.courses) {
        cm.add(
            Map.of(
                "code", c.get("code"),
                "title", c.get("title"),
                "meets", c.get("meets"),
                "writ", c.get("writ")));
      }
      map.put("courses", cm);
      schedOut.add(map);
    }
    outPut.put("schedules", schedOut);
    return outPut;
  }
}