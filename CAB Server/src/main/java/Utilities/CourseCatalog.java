package Utilities;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class CourseCatalog {

  private static final Map<String, Map<String, Object>> coursesByCode = new HashMap<>();

  static {
    try {
      String json = Files.readString(Paths.get("data/courses_formatted.json"));
      Moshi moshi = new Moshi.Builder().build();
      Type rootT = Types.newParameterizedType(Map.class, String.class, Object.class);
      Map<String, Object> root = (Map<String, Object>) moshi.adapter(rootT).fromJson(json);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> results = (List<Map<String, Object>>) root.get("results");
      if (results != null) {
        for (Map<String, Object> course : results) {
          coursesByCode.put((String) course.get("code"), course);
        }
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed to load courses_formatted.json", e);
    }
  }

  public static boolean isWrit(String code) {
    Map<String, Object> course = coursesByCode.get(code);
    return course != null && Boolean.TRUE.equals(course.get("writ"));
  }

  public static Map<String, Object> getCourse(String code) {
    return coursesByCode.get(code);
  }
}