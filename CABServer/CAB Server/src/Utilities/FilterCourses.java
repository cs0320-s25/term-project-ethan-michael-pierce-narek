package Utilities;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map;

public class FilterCourses {
  private static final Moshi moshi = new Moshi.Builder().build();

  public static void main(String[] args) {
    try {
      System.out.println("=== Starting Course Filter ===");
      String chemCoursesJson = filterCourses("202420", "SOC", null, "MWF", true);
      System.out.println("\n=== Final Filtered Courses ===");
      System.out.println(chemCoursesJson);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }

  public static String filterCourses(String term, String deptCode, String time, String day, Boolean writ)
      throws Exception {
    String json = Files.readString(Paths.get("data/courses_formatted.json"));
    JsonAdapter<Object> adapter = moshi.adapter(Object.class);
    Object data = adapter.fromJson(json);

    if (data instanceof Map) {
      Map<String, Object> root = (Map<String, Object>) data;
      List<Object> allCourses = (List<Object>) root.get("results");
      List<Object> filteredCourses = new ArrayList<>();

      System.out.println("\nProcessing courses...");
      int totalCourses = allCourses.size();
      int processed = 0;

      for (Object courseObj : allCourses) {
        processed++;
        Map<String, Object> course = (Map<String, Object>) courseObj;
        String courseTerm = course.get("srcdb").toString();
        String courseCode = course.get("code").toString();
        String meets = course.get("meets").toString();
        String no = course.get("no").toString();

        // Extract day and time
        String[] parts = meets.split(" ", 2);
        String courseDay = parts[0];
        String courseTime = parts.length > 1 ? parts[1] : "TBA";

        // Apply filters
        if (courseTerm.equals(term) &&
            (courseCode.startsWith(deptCode + " ") || deptCode == null) &&
            (courseDay.equals(day) || day == null) &&
            (courseTime.equals(time) || time == null) &&
            (IsWRIT.isClassWRIT(courseCode, courseTerm).equals(writ) || writ == null) &&
            no.startsWith("S")) {

          filteredCourses.add(course);
          System.out.printf("\n[%d/%d] %s: %s (%s %s)",
              processed, totalCourses,
              courseCode, course.get("title"),
              courseDay, courseTime);
          System.out.println("  <<< MATCHES FILTERS");
        }
      }

      // Build result
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("srcdb", term);
      result.put("department", deptCode);
      result.put("count", filteredCourses.size());
      result.put("results", filteredCourses);

      System.out.printf("\nFiltering complete. Found %d matching courses.%n",
          filteredCourses.size());
      return moshi.adapter(Object.class)
          .indent("  ")
          .toJson(result);
    }
    return "{}";
  }
}