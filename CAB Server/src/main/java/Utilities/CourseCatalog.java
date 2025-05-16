package Utilities;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility class that provides access to the Brown University course catalog. This class loads
 * course data from a JSON file and provides methods to query course information efficiently. It
 * serves as a central repository for all course data used by the scheduling application.
 */
public final class CourseCatalog {

  /**
   * Map of course codes to course data objects. This provides quick lookup of course information by
   * course code.
   */
  private static final Map<String, Map<String, Object>> coursesByCode = new HashMap<>();

  /**
   * Static initializer that loads the course catalog data when the class is first used. Reads
   * course data from a JSON file, parses it, and stores it in memory for efficient access. If the
   * data cannot be loaded, a RuntimeException is thrown which will prevent the application from
   * starting with incomplete data.
   */
  static {
    try {
      // Read the JSON file containing course data
      String json = Files.readString(Paths.get("data/courses_formatted.json"));

      // Set up Moshi for JSON parsing
      Moshi moshi = new Moshi.Builder().build();
      Type rootT = Types.newParameterizedType(Map.class, String.class, Object.class);
      Map<String, Object> root = (Map<String, Object>) moshi.adapter(rootT).fromJson(json);

      // Extract course results and populate the lookup map
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> results = (List<Map<String, Object>>) root.get("results");
      if (results != null) {
        for (Map<String, Object> course : results) {
          coursesByCode.put((String) course.get("code"), course);
        }
      }

    } catch (Exception e) {
      // If loading fails, throw a runtime exception to prevent using incomplete data
      throw new RuntimeException("Failed to load courses_formatted.json", e);
    }
  }

  /**
   * Checks if a course has a WRIT designation. The WRIT designation indicates that a course
   * satisfies Brown's writing requirement.
   *
   * @param code The course code to check (e.g., "CSCI 0320")
   * @return true if the course has a WRIT designation, false otherwise or if the course is not
   *     found
   */
  public static boolean isWrit(String code) {
    Map<String, Object> course = coursesByCode.get(code);
    return course != null && Boolean.TRUE.equals(course.get("writ"));
  }

  /**
   * Retrieves detailed information about a specific course.
   *
   * @param code The course code to look up (e.g., "CSCI 0320")
   * @return A map containing the course data, or null if the course is not found
   */
  public static Map<String, Object> getCourse(String code) {
    return coursesByCode.get(code);
  }
}
