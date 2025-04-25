import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.reflect.Type;

public class BrownCourseRequirements {
  private static final Moshi moshi = new Moshi.Builder().build();
  private static final HttpClient httpClient = HttpClient.newHttpClient();

  private static final Map<String, String> COOKIES = Map.of(
      "AMCV_4D6368F454EC41940A4C98A6%40AdobeOrg", "179643557%7CMCIDTS%7C20146%7CMCMID%7C35620687361685143708671614397990958274%7CMCAID%7CNONE%7CMCOPTOUT-1740540362s%7CNONE%7CvVersion%7C5.5.0",
      "__zlcmid", "1QxnVZPFFXDFoMj",
      "acceptcookies", "false",
      "fcsid", "nosfgnrc57f8ur1ju0m0vvv5ap",
      "AMP_572175c4a8", "JTdCJTIyZGV2aWNlSWQlMjIlM0ElMjJhN2Y4ZDQwNS04NzE5LTRjNTQtODM4MC1iOTQ3ZDRhYTE3ZWMlMjIlMkMlMjJ1c2VySWQlMjIlM0ElMjI3ODUxOTk5YTYwN2ZjOWI2Y2EyMDUwNjM1NWVkMTgxN2U4NjcwMDcwNzI4NTIxZjNlY2I1YzIzNTcwNjAzMzA0JTIyJTJDJTIyc2Vzc2lvbklkJTIyJTNBMTc0NDgxNTIxNjQzMiUyQyUyMm9wdE91dCUyMiUzQWZhbHNlJTJDJTIybGFzdEV2ZW50VGltZSUyMiUzQTE3NDQ4MTU1MDUyNjQlMkMlMjJsYXN0RXZlbnRJZCUyMiUzQTQ1JTJDJTIycGFnZUNvdW50ZXIlMjIlM0E3JTdE",
      "AMP_MKTG_572175c4a8", "JTdCJTIycmVmZXJyZXIlMjIlM0ElMjJodHRwcyUzQSUyRiUyRnd3dy5nb29nbGUuY29tJTJGJTIyJTJDJTIycmVmZXJyaW5nX2RvbWFpbiUyMiUzQSUyMnd3dy5nb29nbGUuY29tJTIyJTdE",
      "amplitude_id_9f6c0bb8b82021496164c672a7dc98d6_edmbrown.edu", "eyJkZXZpY2VJZCI6IjdiNGEzZmQ2LTdjMmUtNGZjZC1iMzUxLTRjNTg1OTI1ODE5M1IiLCJ1c2VySWQiOm51bGwsIm9wdE91dCI6ZmFsc2UsInNlc3Npb25JZCI6MTc0NDY2MzMxODU5MywibGFzdEV2ZW50VGltZSI6MTc0NDY2MzMyMzg5OCwiZXZlbnRJZCI6MCwiaWRlbnRpZnlJZCI6MTUsInNlcXVlbmNlTnVtYmVyIjoxNX0=",
      "IDMSESSID", "5F4F6FE688A6974A45BB5DDF724B25B1D43F9B1F2904B921CD5E4D2BB297C5AC19E573A1A6BE903A2A963193973A2681",
      "TS01b3a32b", "014b44e76b2bfffa4bf11f672f482dda08cb75db42a613534f113167d5bf454488e2a1502795632863ba48809e01be925dd9ca685b"
  );

  private static final Map<String, String> HEADERS = Map.ofEntries(
      Map.entry("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:137.0) Gecko/20100101 Firefox/137.0"),
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
      Map.entry("Priority", "u=0")
  );

  public static void main(String[] args) {
    try {
      getCourseRequirements("CHEM 0350", "202420");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void getCourseRequirements(String courseCode, String term) throws Exception {
    String searchUrl = "https://cab.brown.edu/api/?page=fose&route=search";
    String detailsUrl = "https://cab.brown.edu/api/?page=fose&route=details";

    // Step 1: Search for the course code
    Map<String, Object> searchPayload = new HashMap<>();
    Map<String, String> other = new HashMap<>();
    other.put("srcdb", term);
    searchPayload.put("other", other);

    searchPayload.put("criteria", new Object[] {
        Map.of("field", "code", "value", courseCode),
        Map.of("field", "is_ind_study", "value", "N"),
        Map.of("field", "is_canc", "value", "N")
    });

    HttpRequest searchRequest = buildRequest(searchUrl, searchPayload);
    HttpResponse<String> searchResponse = httpClient.send(searchRequest, BodyHandlers.ofString());

    // Parse response with Moshi
    Type searchResponseType = Types.newParameterizedType(Map.class, String.class, Object.class);
    JsonAdapter<Map<String, Object>> searchAdapter = moshi.adapter(searchResponseType);
    Map<String, Object> searchData = searchAdapter.fromJson(searchResponse.body());

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> results = (List<Map<String, Object>>) searchData.get("results");
    if (results == null || results.isEmpty()) {
      System.out.printf("No results found for %s%n", courseCode);
      return;
    }

    // Step 2: Get detailed course info
    String crn = (String) results.get(0).get("crn");
    String srcdb = (String) results.get(0).get("srcdb");

    Map<String, String> detailPayload = Map.of(
        "srcdb", srcdb,
        "crn", crn,
        "group", "code:" + courseCode
    );

    HttpRequest detailRequest = buildRequest(detailsUrl, detailPayload);
    HttpResponse<String> detailResponse = httpClient.send(detailRequest, BodyHandlers.ofString());

    // Parse detail response with Moshi
    JsonAdapter<Map<String, Object>> detailAdapter = moshi.adapter(searchResponseType);
    Map<String, Object> detailData = detailAdapter.fromJson(detailResponse.body());

    // Step 3: Extract requirements
    String prereqHtml = (String) detailData.get("registration_restrictions");
    boolean permissionRequired = "Y".equals(detailData.get("permreq"));
    String description = (String) detailData.get("description");

    System.out.printf("%n %s: Requirements%n", courseCode);
    if (prereqHtml != null && !prereqHtml.isEmpty()) {
      System.out.println("From registration restrictions:");
      System.out.println(prereqHtml);
    } else {
      System.out.println("No formal registration restrictions.");
    }

    if (permissionRequired) {
      System.out.println("Instructor or department permission is required.");
    }

    if (description != null &&
        (description.toLowerCase().contains("prerequisite") ||
            description.toLowerCase().contains("recommended"))) {
      System.out.println("\nFrom course description (may be soft prereqs):");
      System.out.println(description);
    }
  }

  private static HttpRequest buildRequest(String url, Object payload) throws IOException {
    String cookies = COOKIES.entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .reduce((a, b) -> a + "; " + b)
        .orElse("");

    JsonAdapter<Object> jsonAdapter = moshi.adapter(Object.class);
    String jsonPayload = jsonAdapter.toJson(payload);

    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .headers(buildHeadersArray())
        .header("Cookie", cookies)
        .POST(BodyPublishers.ofString(jsonPayload));

    return builder.build();
  }

  private static String[] buildHeadersArray() {
    return HEADERS.entrySet().stream()
        .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
        .toArray(String[]::new);
  }
}


