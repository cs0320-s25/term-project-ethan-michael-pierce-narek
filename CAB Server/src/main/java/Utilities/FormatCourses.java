package Utilities;

import com.squareup.moshi.Moshi;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FormatCourses {
  public static void main(String[] args) {
    try {
      // Define paths
      String inputPath = "data/F25courses.json";
      String outputPath = "data/courses_formatted.json";

      // Verify input file exists
      if (!Files.exists(Paths.get(inputPath))) {
        System.err.println(
            "Error: Input file not found at " + Paths.get(inputPath).toAbsolutePath());
        System.err.println("Current working directory: " + System.getProperty("user.dir"));
        return;
      }

      // Read and format JSON
      String json = new String(Files.readAllBytes(Paths.get(inputPath)));
      Moshi moshi = new Moshi.Builder().build();
      Object parsed = moshi.adapter(Object.class).fromJson(json);

      if (parsed != null) {
        String pretty = moshi.adapter(Object.class).indent("  ").toJson(parsed);
        Files.writeString(Paths.get(outputPath), pretty);
        System.out.println(
            "Successfully formatted JSON to " + Paths.get(outputPath).toAbsolutePath());
      }
    } catch (Exception e) {
      System.err.println("Error processing JSON: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
