package Handlers;

import Scheduler.ScheduleGenerator;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.lang.reflect.Type;
import java.util.*;
import spark.Request;
import spark.Response;
import spark.Route;

/** This handler processes requests to generate optimal schedules based on user preferences. */
public class GenerateScheduleHandler implements Route {

  private static final Moshi moshi = new Moshi.Builder().build();

  @Override
  public Object handle(Request request, Response response) throws Exception {
    try {
      // Parse request parameters
      String term = request.queryParams("term");
      if (term == null) {
        throw new IllegalArgumentException("Term parameter is required");
      }

      // Parse JSON body from request
      String requestBody = request.body();
      Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
      JsonAdapter<Map<String, Object>> jsonAdapter = moshi.adapter(type);
      Map<String, Object> requestData = jsonAdapter.fromJson(requestBody);

      if (requestData == null) {
        throw new IllegalArgumentException("Invalid request body");
      }

      // Extract user profile
      int classesPerSemester = ((Double) requestData.get("classesPerSemester")).intValue();
      List<String> coursesTaken = (List<String>) requestData.get("coursesTaken");
      List<String> remainingRequired = (List<String>) requestData.get("remainingRequired");

      // Extract preferences
      List<String> necessaryCourses = (List<String>) requestData.get("necessaryCourses");
      Set<String> availableTimes = new HashSet<>((List<String>) requestData.get("availableTimes"));

      // Parse day availability
      Map<String, Object> dayAvailabilityObj =
          (Map<String, Object>) requestData.get("dayAvailability");
      Map<String, Boolean> dayAvailability = new HashMap<>();
      for (Map.Entry<String, Object> entry : dayAvailabilityObj.entrySet()) {
        dayAvailability.put(entry.getKey(), (Boolean) entry.getValue());
      }

      // Parse day balance preference
      Map<String, Object> dayBalanceObj = (Map<String, Object>) requestData.get("dayBalance");
      ScheduleGenerator.DayBalance dayBalance =
          new ScheduleGenerator.DayBalance(
              ((Double) dayBalanceObj.get("mwfCount")).intValue(),
              ((Double) dayBalanceObj.get("tthCount")).intValue());

      int requiredCoursesThisSemester =
          ((Double) requestData.get("requiredCoursesThisSemester")).intValue();
      List<String> preferredDepts = (List<String>) requestData.get("preferredDepts");
      boolean needWRIT = (Boolean) requestData.get("needWRIT");

      // Initialize the schedule generator
      ScheduleGenerator generator =
          new ScheduleGenerator(
              classesPerSemester,
              coursesTaken,
              remainingRequired,
              necessaryCourses,
              availableTimes,
              dayAvailability,
              dayBalance,
              requiredCoursesThisSemester,
              preferredDepts,
              needWRIT);

      // Load course data
      generator.loadCourseData(term);

      // Filter courses based on availability and prerequisites
      generator.filterCourses(term);

      try {
        // Verify necessary courses have prerequisites met
        List<String> missingPrereqs = generator.verifyNecessaryCourses(term);

        if (!missingPrereqs.isEmpty()) {
          // Return an error response about missing prerequisites
          Map<String, Object> errorResponse = new HashMap<>();
          errorResponse.put("success", false);
          errorResponse.put("error", "Missing prerequisites");
          errorResponse.put("missingPrerequisites", missingPrereqs);

          String jsonResponse = moshi.adapter(Map.class).toJson(errorResponse);
          response.type("application/json");
          return jsonResponse;
        }

        // Generate schedule options
        List<ScheduleGenerator.Schedule> schedules = generator.generateSchedules(term);

        // Build response JSON
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("schedulesCount", schedules.size());

        List<Map<String, Object>> scheduleDataList = new ArrayList<>();
        for (ScheduleGenerator.Schedule schedule : schedules) {
          Map<String, Object> scheduleData = new HashMap<>();
          scheduleData.put("score", schedule.score);
          scheduleData.put("courses", schedule.courses);

          // Add the day balance information
          ScheduleGenerator.DayBalance scheduleBalance = schedule.getDayBalance();
          Map<String, Integer> balanceData = new HashMap<>();
          balanceData.put("mwfCount", scheduleBalance.mwfCount);
          balanceData.put("tthCount", scheduleBalance.tthCount);
          scheduleData.put("dayBalance", balanceData);

          scheduleDataList.add(scheduleData);
        }

        successResponse.put("schedules", scheduleDataList);

        String jsonResponse = moshi.adapter(Map.class).toJson(successResponse);
        response.type("application/json");
        return jsonResponse;

      } catch (IllegalStateException e) {
        // This handles other validation errors from the generator
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", e.getMessage());

        String jsonResponse = moshi.adapter(Map.class).toJson(errorResponse);
        response.type("application/json");
        return jsonResponse;
      }

    } catch (Exception e) {
      e.printStackTrace();
      response.status(500);
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("success", "false");
      errorResponse.put("error", e.getMessage());

      return moshi.adapter(Map.class).toJson(errorResponse);
    }
  }
}
