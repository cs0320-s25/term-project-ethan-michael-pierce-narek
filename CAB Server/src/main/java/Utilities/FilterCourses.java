package Utilities;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map;

/**
 * Utility class for filtering courses based on various criteria. This class provides functionality
 * to filter Brown University courses by term, department, meeting time, meeting day, and WRIT
 * designation.
 */
public class FilterCourses {
  /** Moshi instance for JSON serialization/deserialization */
  private static final Moshi moshi = new Moshi.Builder().build();

  /**
   * Main method for running the course filter as a standalone utility. Demonstrates filtering
   * Sociology courses offered on MWF with WRIT designation for the Spring 2025 term.
   *
   * @param args Command line arguments (not used)
   */
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

  /**
   * Filters courses based on specified criteria. This method loads course data from a JSON file and
   * filters it according to the provided parameters. All parameters except term are optional.
   *
   * @param term The term code to filter by (e.g., "202420" for Spring 2025)
   * @param deptCode The department code to filter by (e.g., "CSCI"), or null for all departments
   * @param time The specific time block to filter by (e.g., "10-11:20a"), or null for all times
   * @param day The specific day(s) to filter by (e.g., "MWF", "TTh"), or null for all days
   * @param writ Boolean indicating whether to include only WRIT courses (true), non-WRIT courses
   *     (false), or both (null)
   * @return A JSON string containing the filtered courses
   * @throws Exception If an error occurs during file reading or JSON processing
   */
  public static String filterCourses(
      String term, String deptCode, String time, String day, Boolean writ) throws Exception {
    // Read course data from JSON file
    String json = Files.readString(Paths.get("data/courses_formatted.json"));
    JsonAdapter<Object> adapter = moshi.adapter(Object.class);
    Object data = adapter.fromJson(json);

    if (data instanceof Map) {
      Map<String, Object> root = (Map<String, Object>) data;
      List<Object> allCourses = (List<Object>) root.get("results");
      List<Object> filteredCourses = new ArrayList<>();

      // Log processing start
      System.out.println("\nProcessing courses...");
      int totalCourses = allCourses.size();
      int processed = 0;

      // Process each course
      for (Object courseObj : allCourses) {
        processed++;
        Map<String, Object> course = (Map<String, Object>) courseObj;
        String courseTerm = course.get("srcdb").toString();
        String courseCode = course.get("code").toString();
        String meets = course.get("meets").toString();
        String no = course.get("no").toString();

        // Extract day and time from meeting string
        String[] parts = meets.split(" ", 2);
        String courseDay = parts[0];
        String courseTime = parts.length > 1 ? parts[1] : "TBA";

        // Apply all filters
        if (courseTerm.equals(term)
            && (courseCode.startsWith(deptCode + " ") || deptCode == null)
            && (courseDay.equals(day) || day == null)
            && (courseTime.equals(time) || time == null)
            && (writ == null || CourseCatalog.isWrit(courseCode) == writ)
            && no.startsWith("S")) { // Only include primary sections

          // Add matching course to filtered list
          filteredCourses.add(course);

          // Log match
          System.out.printf(
              "\n[%d/%d] %s: %s (%s %s)",
              processed, totalCourses, courseCode, course.get("title"), courseDay, courseTime);
          System.out.println("  <<< MATCHES FILTERS");
        }
      }

      // Build result object
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("srcdb", term);
      result.put("department", deptCode);
      result.put("count", filteredCourses.size());
      result.put("results", filteredCourses);

      // Log summary and return formatted JSON
      System.out.printf(
          "\nFiltering complete. Found %d matching courses.%n", filteredCourses.size());
      return moshi.adapter(Object.class).indent("  ").toJson(result);
    }
    return "{}"; // Return empty JSON object if data is not in expected format
  }
}
