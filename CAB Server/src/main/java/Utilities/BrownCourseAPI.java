package Utilities;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * API client for fetching course data from Brown University's course catalog. This class provides
 * functionality to retrieve course information from the Courses@Brown system, process it, and save
 * it to a local JSON file for use by the scheduling application.
 */
public class BrownCourseAPI {

  /** Moshi instance for JSON serialization/deserialization */
  private static final Moshi moshi = new Moshi.Builder().build();

  /** HTTP client for making requests to the Brown API */
  private static final HttpClient httpClient = HttpClient.newHttpClient();

  /** Directory where course data is stored */
  private static final String DATA_DIR = "data";

  /** Filename for storing formatted course data */
  private static final String COURSES_FILE = "courses_formatted.json";

  /**
   * HTTP headers required for Courses@Brown API requests. These simulate a browser request to avoid
   * being blocked.
   */
  private static final Map<String, String> HEADERS =
      Map.ofEntries(
          Map.entry(
              "User-Agent",
              "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:137.0) Gecko/20100101 Firefox/137.0"),
          Map.entry("Accept", "application/json, text/javascript, */*; q=0.01"),
          Map.entry("Accept-Language", "en-US,en;q=0.5"),
          Map.entry("Content-Type", "application/json"),
          Map.entry("X-Requested-With", "XMLHttpRequest"),
          Map.entry("Origin", "https://cab.brown.edu"),
          Map.entry("Referer", "https://cab.brown.edu/"),
          Map.entry("Sec-Fetch-Dest", "empty"),
          Map.entry("Sec-Fetch-Mode", "cors"),
          Map.entry("Sec-Fetch-Site", "same-origin"),
          Map.entry("DNT", "1"),
          Map.entry("Sec-GPC", "1"),
          Map.entry("Priority", "u=0"));

  /**
   * Cookie header required for Courses@Brown API requests. This is needed to authenticate requests
   * to the API.
   */
  private static final String COOKIE_HEADER =
      "AMCV_4D6368F454EC41940A4C98A6%40AdobeOrg=179643557%7CMCIDTS%7C20146%7CMCMID%7C35620687361685143708671614397990958274%7CMCAID%7CNONE%7CMCOPTOUT-1740540362s%7CNONE%7CvVersion%7C5.5.0; "
          + "__zlcmid=1QxnVZPFFXDFoMj; acceptcookies=false; fcsid=nosfgnrc57f8ur1ju0m0vvv5ap; "
          + "AMP_572175c4a8=JTdCJTIyZGV2aWNlSWQlMjIlM0ElMjJhN2Y4ZDQwNS04NzE5LTRjNTQtODM4MC1iOTQ3ZDRhYTE3ZWMlMjIlMkMlMjJ1c2VySWQlMjIlM0ElMjI3ODUxOTk5YTYwN2ZjOWI2Y2EyMDUwNjM1NWVkMTgxN2U4NjcwMDcwNzI4NTIxZjNlY2I1YzIzNTcwNjAzMzA0JTIyJTJDJTIyc2Vzc2lvbklkJTIyJTNBMTc0NTI2NDU2MTk4NSUyQyUyMm9wdE91dCUyMiUzQWZhbHNlJTJDJTIybGFzdEV2ZW50VGltZSUyMiUzQTE3NDUyNjQ2MDMyMzElMkMlMjJsYXN0RXZlbnRJZCUyMiUzQTI4JTJDJTIycGFnZUNvdW50ZXIlMjIlM0E4JTdE";

  /**
   * Entry point for running the API client as a standalone application. Fetches course data for the
   * Spring 2025 term.
   *
   * @param args Command line arguments (not used)
   * @throws Exception If an error occurs during the fetch process
   */
  public static void main(String[] args) throws Exception {
    fetchAndSaveCourses("202420"); // Spring 2025 term code
  }

  /**
   * Fetches course data for a specified term, processes it, and saves it to a JSON file. This
   * method handles the complete process of retrieving and processing course data, including: -
   * Fetching basic course information - Retrieving detailed information for each course -
   * Extracting WRIT designation and prerequisites - Removing duplicate sections - Formatting and
   * saving the data
   *
   * @param term The term code to fetch courses for (e.g., "202420" for Spring 2025)
   * @throws Exception If an error occurs during the fetch, process, or save steps
   */
  public static void fetchAndSaveCourses(String term) throws Exception {
    // Create data directory if it doesn't exist
    Path dataDir = Paths.get(DATA_DIR);
    if (!Files.exists(dataDir)) Files.createDirectories(dataDir);

    // URL for the course search API
    String searchUrl = "https://cab.brown.edu/api/?page=fose&route=search"; // basic info

    // Query body to fetch all non-independent-study, non-cancelled courses
    String searchBody =
        String.format(
            "{\"other\":{\"srcdb\":\"%s\"},\"criteria\":[{\"field\":\"is_ind_study\",\"value\":\"N\"},{\"field\":\"is_canc\",\"value\":\"N\"}]}",
            term);

    // Build the HTTP request for the search API
    HttpRequest searchReq =
        HttpRequest.newBuilder()
            .uri(URI.create(searchUrl))
            .headers(headersArray())
            .header("Cookie", COOKIE_HEADER)
            .POST(BodyPublishers.ofString(searchBody))
            .build();

    // Send the request and get the response
    HttpResponse<String> searchResp = httpClient.send(searchReq, BodyHandlers.ofString());

    // Set up JSON adapter for parsing the response
    Type mapT = Types.newParameterizedType(Map.class, String.class, Object.class);
    JsonAdapter<Map<String, Object>> mapAdapter = moshi.adapter(mapT);

    // Parse the response JSON
    Map<String, Object> root = mapAdapter.fromJson(searchResp.body());

    // Extract the results list, or use an empty list if null
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> raw = (List<Map<String, Object>>) root.get("results");
    if (raw == null) raw = new ArrayList<>();

    // Map to store unique courses by course code
    Map<String, Map<String, Object>> coursesByCode = new LinkedHashMap<>();

    // Remove duplicate courses (keeping only one section per course)
    for (Map<String, Object> course : raw) {
      String code = (String) course.get("code");
      coursesByCode.putIfAbsent(code, course);
    }

    // Convert the map values back to a list
    List<Map<String, Object>> results = new ArrayList<>(coursesByCode.values());

    // Extract all department codes for use in parsing prerequisites
    Set<String> deptCodes = new HashSet<>();
    for (Map<String, Object> c : results) {
      String code = (String) c.get("code");
      if (code != null && code.contains(" ")) {
        deptCodes.add(code.substring(0, code.indexOf(' ')));
      }
    }

    // Fetch detailed information for each course
    int count = 0;
    for (Map<String, Object> course : results) {
      count++;
      String crn = (String) course.get("crn");
      String srcdb = (String) course.get("srcdb");

      try {
        String courseCode = (String) course.get("code");
        // Fetch detailed course information
        Map<String, Object> details = fetchDetails(crn, srcdb);

        // Check for WRIT designation
        boolean hasWrit = false;
        String attr = (String) details.get("attr_html");
        if (attr != null && attr.toUpperCase().contains("WRIT")) hasWrit = true;
        course.put("writ", hasWrit);

        // Extract prerequisites
        String restrictions = (String) details.get("registration_restrictions");
        String prereqSentence = extractPrereq(null, restrictions);
        course.put("prereq", prereqSentence);

        // Extract prerequisite groups for programmatic use
        List<List<String>> prereqGroups = extractPrereqGroups(restrictions, deptCodes);
        course.put("prereqGroups", prereqGroups);

      } catch (Exception e) {
        // Set default values if fetching details fails
        course.put("writ", false);
        course.put("prereq", "");
      }
    }

    // Create the final JSON object with de-duplicated courses
    Map<String, Object> uniqueRoot = new LinkedHashMap<>(root);
    uniqueRoot.put("results", new ArrayList<>(coursesByCode.values()));
    uniqueRoot.put("count", coursesByCode.size());

    // Write the formatted JSON to file
    Path out = Paths.get(DATA_DIR, COURSES_FILE);
    Files.writeString(out, mapAdapter.indent("  ").toJson(uniqueRoot));
  }

  /**
   * Retrieves detailed information for a specific course. Makes an API request to get full course
   * details using the CRN and term code.
   *
   * @param crn The Course Registration Number
   * @param term The term code (e.g., "202420")
   * @return A map containing the detailed course information
   * @throws IOException If an error occurs during the HTTP request
   * @throws InterruptedException If the HTTP request is interrupted
   */
  private static Map<String, Object> fetchDetails(String crn, String term)
      throws IOException, InterruptedException {
    String url = "https://cab.brown.edu/api/?page=fose&route=details"; // full details API
    Map<String, String> body = Map.of("srcdb", term, "crn", crn, "group", "crn:" + crn);

    // Build the HTTP request
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .headers(headersArray())
            .header("Cookie", COOKIE_HEADER)
            .POST(BodyPublishers.ofString(moshi.adapter(Object.class).toJson(body)))
            .build();

    // Send the request and parse the response
    HttpResponse<String> resp = httpClient.send(req, BodyHandlers.ofString());
    Type t = Types.newParameterizedType(Map.class, String.class, Object.class);
    return (Map<String, Object>) moshi.adapter(t).fromJson(resp.body());
  }

  /**
   * Extracts prerequisite text from course information. Looks for the word "prerequisite" in the
   * text and extracts the sentence containing it.
   *
   * @param description Course description text (may be null)
   * @param restrictions Course registration restrictions text (may be null)
   * @return The extracted prerequisite sentence, or an empty string if none found
   */
  private static String extractPrereq(String description, String restrictions) {
    String text =
        (description == null ? "" : description) + " " + (restrictions == null ? "" : restrictions);
    int idx = text.toLowerCase().indexOf("prerequisite");
    if (idx == -1) return "";

    // Find the end of the sentence (period after "prerequisite")
    int end = text.indexOf('.', idx);

    // Extract the sentence containing prerequisites
    String sent = text.substring(idx, end == -1 ? text.length() : end);

    // Remove the word "prerequisite" or "prerequisites:" and trim whitespace
    return sent.replaceFirst("(?i)prerequisite[s]?:", "").trim();
  }

  /**
   * Extracts structured prerequisite information from HTML text. Parses the text to identify course
   * codes and organize them into logical groups. Each outer list represents an "AND" condition
   * (must satisfy all), while each inner list represents an "OR" condition (must satisfy one).
   *
   * @param html HTML text containing prerequisite information
   * @param depts Set of valid department codes for context
   * @return Nested list structure of prerequisite course codes
   */
  private static List<List<String>> extractPrereqGroups(String html, Set<String> depts) {
    List<List<String>> groups = new ArrayList<>();
    if (html == null || html.isBlank()) return groups;

    // Clean up the HTML text
    String text =
        html.replaceAll("<[^>]+>", " ") // Remove HTML tags
            .replaceAll("%20", " ") // Replace URL encodings
            .replaceAll("[,;]", " , "); // Normalize commas and semicolons

    // Split by "and" to get requirement groups
    for (String segment : text.split("(?i)\\band\\b")) {
      List<String> orSet = new ArrayList<>();
      String[] tokens = segment.trim().split("\\s+");
      String currentDept = null;

      for (String token : tokens) {
        if (token.equals(",")) continue;

        // Check if token is a department code
        if (depts.contains(token.toUpperCase())) {
          currentDept = token.toUpperCase();
          continue;
        }

        // Check if token is just a course number (e.g., "0320")
        if (token.matches("\\d{4}[A-Z]?")) {
          if (currentDept != null) {
            orSet.add(currentDept + " " + token);
          }
          continue;
        }

        // Check if token is a full course code (e.g., "CSCI0320")
        if (token.matches("[A-Z]{3,4}\\s?\\d{4}[A-Z]?")) {
          String dept = token.substring(0, token.length() - 4).trim().toUpperCase();
          String num = token.substring(token.length() - 4);
          currentDept = dept; // Update department context
          orSet.add(dept + " " + num);
        }
      }

      // Add non-empty OR groups to the AND list
      if (!orSet.isEmpty()) groups.add(orSet);
    }
    return groups;
  }

  /**
   * Converts the headers map to an array format required by HttpRequest. Each key-value pair in the
   * map becomes two consecutive elements in the array.
   *
   * @return Array of header strings in key,value order
   */
  private static String[] headersArray() {
    return HEADERS.entrySet().stream()
        .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
        .toArray(String[]::new);
  }
}
