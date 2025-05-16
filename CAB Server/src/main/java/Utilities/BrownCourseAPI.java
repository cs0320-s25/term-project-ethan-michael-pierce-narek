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

public class BrownCourseAPI {

  private static final Moshi moshi = new Moshi.Builder().build();
  private static final HttpClient httpClient = HttpClient.newHttpClient();

  private static final String DATA_DIR = "data";
  private static final String COURSES_FILE = "courses_formatted.json";

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

  private static final String COOKIE_HEADER =
      "AMCV_4D6368F454EC41940A4C98A6%40AdobeOrg=179643557%7CMCIDTS%7C20146%7CMCMID%7C35620687361685143708671614397990958274%7CMCAID%7CNONE%7CMCOPTOUT-1740540362s%7CNONE%7CvVersion%7C5.5.0; "
          + "__zlcmid=1QxnVZPFFXDFoMj; acceptcookies=false; fcsid=nosfgnrc57f8ur1ju0m0vvv5ap; "
          + "AMP_572175c4a8=JTdCJTIyZGV2aWNlSWQlMjIlM0ElMjJhN2Y4ZDQwNS04NzE5LTRjNTQtODM4MC1iOTQ3ZDRhYTE3ZWMlMjIlMkMlMjJ1c2VySWQlMjIlM0ElMjI3ODUxOTk5YTYwN2ZjOWI2Y2EyMDUwNjM1NWVkMTgxN2U4NjcwMDcwNzI4NTIxZjNlY2I1YzIzNTcwNjAzMzA0JTIyJTJDJTIyc2Vzc2lvbklkJTIyJTNBMTc0NTI2NDU2MTk4NSUyQyUyMm9wdE91dCUyMiUzQWZhbHNlJTJDJTIybGFzdEV2ZW50VGltZSUyMiUzQTE3NDUyNjQ2MDMyMzElMkMlMjJsYXN0RXZlbnRJZCUyMiUzQTI4JTJDJTIycGFnZUNvdW50ZXIlMjIlM0E4JTdE";

  public static void main(String[] args) throws Exception {
    fetchAndSaveCourses("202420"); // Spring 2025 term code
  }

  public static void fetchAndSaveCourses(String term) throws Exception {

    Path dataDir = Paths.get(DATA_DIR);
    if (!Files.exists(dataDir)) Files.createDirectories(dataDir);

    String searchUrl = "https://cab.brown.edu/api/?page=fose&route=search"; // basic info
    String searchBody =
        String.format(
            "{\"other\":{\"srcdb\":\"%s\"},\"criteria\":[{\"field\":\"is_ind_study\",\"value\":\"N\"},{\"field\":\"is_canc\",\"value\":\"N\"}]}",
            term);

    HttpRequest searchReq =
        HttpRequest.newBuilder()
            .uri(URI.create(searchUrl))
            .headers(headersArray())
            .header("Cookie", COOKIE_HEADER)
            .POST(BodyPublishers.ofString(searchBody))
            .build();

    HttpResponse<String> searchResp = httpClient.send(searchReq, BodyHandlers.ofString());

    Type mapT = Types.newParameterizedType(Map.class, String.class, Object.class);
    JsonAdapter<Map<String, Object>> mapAdapter = moshi.adapter(mapT);

    Map<String, Object> root = mapAdapter.fromJson(searchResp.body());

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> raw = (List<Map<String, Object>>) root.get("results");
    if (raw == null) raw = new ArrayList<>();

    Map<String, Map<String, Object>> coursesByCode = new LinkedHashMap<>();

    // Removes duplicate courses, important because of sections
    for (Map<String, Object> course : raw) {
      String code = (String) course.get("code");
      coursesByCode.putIfAbsent(code, course);
    }

    List<Map<String, Object>> results = new ArrayList<>(coursesByCode.values());

    // Extract department codes
    Set<String> deptCodes = new HashSet<>();
    for (Map<String, Object> c : results) {
      String code = (String) c.get("code");
      if (code != null && code.contains(" ")) {
        deptCodes.add(code.substring(0, code.indexOf(' ')));
      }
    }

    int count = 0;
    for (Map<String, Object> course : results) {
      count++;
      String crn = (String) course.get("crn");
      String srcdb = (String) course.get("srcdb");

      try {
        String courseCode = (String) course.get("code");
        Map<String, Object> details = fetchDetails(crn, srcdb);

        boolean hasWrit = false;
        String attr = (String) details.get("attr_html");
        if (attr != null && attr.toUpperCase().contains("WRIT")) hasWrit = true;
        course.put("writ", hasWrit);

        String restrictions = (String) details.get("registration_restrictions");

        String prereqSentence = extractPrereq(null, restrictions);
        course.put("prereq", prereqSentence);

        List<List<String>> prereqGroups = extractPrereqGroups(restrictions, deptCodes);
        course.put("prereqGroups", prereqGroups);

      } catch (Exception e) {
        course.put("writ", false);
        course.put("prereq", "");
      }
    }

    Map<String, Object> uniqueRoot = new LinkedHashMap<>(root);
    uniqueRoot.put("results", new ArrayList<>(coursesByCode.values()));
    uniqueRoot.put("count", coursesByCode.size());

    Path out = Paths.get(DATA_DIR, COURSES_FILE);
    Files.writeString(out, mapAdapter.indent("  ").toJson(uniqueRoot));
  }

  // Retrieves course information for a particular course specified by crn and term
  private static Map<String, Object> fetchDetails(String crn, String term)
      throws IOException, InterruptedException {
    String url = "https://cab.brown.edu/api/?page=fose&route=details"; // full details
    Map<String, String> body = Map.of("srcdb", term, "crn", crn, "group", "crn:" + crn);

    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .headers(headersArray())
            .header("Cookie", COOKIE_HEADER)
            .POST(BodyPublishers.ofString(moshi.adapter(Object.class).toJson(body)))
            .build();

    HttpResponse<String> resp = httpClient.send(req, BodyHandlers.ofString());
    Type t = Types.newParameterizedType(Map.class, String.class, Object.class);
    return (Map<String, Object>) moshi.adapter(t).fromJson(resp.body());
  }

  private static String extractPrereq(String description, String restrictions) {
    String text = (description == null ? "" : description) + " " + (restrictions == null ? "" : restrictions);
    int idx = text.toLowerCase().indexOf("prerequisite");
    if (idx == -1) return "";
    int end = text.indexOf('.', idx); // Look for first period after the word prerequisite
    String sent = text.substring(idx, end == -1 ? text.length() : end); // Substring from prereq to end of sentence
    return sent.replaceFirst("(?i)prerequisite[s]?:", "").trim(); // Remove word prerequisite
  }

  private static List<List<String>> extractPrereqGroups(String html, Set<String> depts) {

    List<List<String>> groups = new ArrayList<>();
    if (html == null || html.isBlank()) return groups;

    String text =
        html.replaceAll("<[^>]+>", " ") // Remove html tags
            .replaceAll("%20", " ") // Replace url encodings
            .replaceAll("[,;]", " , "); // Normalize commas and semicolons, replace with comma surrounded by spaces

    for (String segment : text.split("(?i)\\band\\b")) { // Isolates text into parts seperated by "and"

      List<String> orSet = new ArrayList<>();
      String[] tokens = segment.trim().split("\\s+"); // Remove white space, create array of tokens
      String currentDept = null;

      for (String token : tokens) {
        if (token.equals(",")) continue;

        if (depts.contains(token.toUpperCase())) {
          currentDept = token.toUpperCase(); // Set current department to this department for context
          continue;
        }

        if (token.matches("\\d{4}[A-Z]?")) { // Check if token is only 4 digit course number, ex: '0320'
          if (currentDept != null) {
            orSet.add(currentDept + " " + token);
          }
          continue;
        }

        if (token.matches("[A-Z]{3,4}\\s?\\d{4}[A-Z]?")) { // Check if token is a full course, ex: 'CSCI 0320'
          String dept = token.substring(0, token.length() - 4).trim().toUpperCase();
          String num = token.substring(token.length() - 4);
          currentDept = dept; // Update department context
          orSet.add(dept + " " + num);
        }
      }
      if (!orSet.isEmpty()) groups.add(orSet);
    }
    return groups;
  }

  private static String[] headersArray() {
    return HEADERS.entrySet().stream()
        .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
        .toArray(String[]::new);
  }
}