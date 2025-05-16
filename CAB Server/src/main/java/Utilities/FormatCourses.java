package Utilities;

import com.squareup.moshi.Moshi;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility class for formatting raw course data JSON files.
 * This class reads a raw JSON file containing Brown University course data,
 * parses it, and writes it back in a consistently formatted, pretty-printed format
 * for better readability and processing by other components of the system.
 */
public class FormatCourses {
  /**
   * Main method that performs the JSON formatting process.
   * The process includes:
   * 1. Checking if the input file exists
   * 2. Reading and parsing the JSON file
   * 3. Pretty-printing the JSON with consistent indentation
   * 4. Writing the formatted JSON to the output file
   *
   * @param args Command line arguments (not used)
   */
  public static void main(String[] args) {
    try {
      // Define input and output file paths
      String inputPath = "data/F25courses.json";
      String outputPath = "data/courses_formatted.json";

      // Verify that the input file exists
      if (!Files.exists(Paths.get(inputPath))) {
        System.err.println(
            "Error: Input file not found at " + Paths.get(inputPath).toAbsolutePath());
        System.err.println("Current working directory: " + System.getProperty("user.dir"));
        return;
      }

      // Read the raw JSON file content
      String json = new String(Files.readAllBytes(Paths.get(inputPath)));

      // Set up Moshi for JSON parsing and formatting
      Moshi moshi = new Moshi.Builder().build();

      // Parse the JSON to ensure it's valid
      Object parsed = moshi.adapter(Object.class).fromJson(json);

      // Format and write the JSON if parsing was successful
      if (parsed != null) {
        // Create pretty-printed JSON with 2-space indentation
        String pretty = moshi.adapter(Object.class).indent("  ").toJson(parsed);

        // Write the formatted JSON to the output file
        Files.writeString(Paths.get(outputPath), pretty);

        // Print success message with absolute path for clarity
        System.out.println(
            "Successfully formatted JSON to " + Paths.get(outputPath).toAbsolutePath());
      }
    } catch (Exception e) {
      // Handle any errors that occur during processing
      System.err.println("Error processing JSON: " + e.getMessage());
      e.printStackTrace();
    }
  }
}