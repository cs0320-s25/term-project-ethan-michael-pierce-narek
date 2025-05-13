package Utilities;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

public class BrownCourseAPI {

  private static final String DATA_DIR = "data";
  private static final String COURSES_FILE = "F25Courses.json";

  public static void main(String[] args) throws Exception {
    // Fetch and save all courses for Summer 2024 (term 202420)
    fetchAndSaveCourses("202410");
  }

  public static void fetchAndSaveCourses(String term) throws Exception {
    // Create data directory if it doesn't exist
    Path dataDir = Paths.get(DATA_DIR);
    if (!Files.exists(dataDir)) {
      Files.createDirectories(dataDir);
    }

    // Create HttpClient
    HttpClient client = HttpClient.newHttpClient();

    // Prepare cookies and headers (same as before)
    String cookies = "AMCV_4D6368F454EC41940A4C98A6%40AdobeOrg=179643557%7CMCIDTS%7C20146%7CMCMID%7C35620687361685143708671614397990958274%7CMCAID%7CNONE%7CMCOPTOUT-1740540362s%7CNONE%7CvVersion%7C5.5.0; " +
        "__zlcmid=1QxnVZPFFXDFoMj; " +
        "acceptcookies=false; " +
        "fcsid=nosfgnrc57f8ur1ju0m0vvv5ap; " +
        "AMP_572175c4a8=JTdCJTIyZGV2aWNlSWQlMjIlM0ElMjJhN2Y4ZDQwNS04NzE5LTRjNTQtODM4MC1iOTQ3ZDRhYTE3ZWMlMjIlMkMlMjJ1c2VySWQlMjIlM0ElMjI3ODUxOTk5YTYwN2ZjOWI2Y2EyMDUwNjM1NWVkMTgxN2U4NjcwMDcwNzI4NTIxZjNlY2I1YzIzNTcwNjAzMzA0JTIyJTJDJTIyc2Vzc2lvbklkJTIyJTNBMTc0NTQxNzQyNDE2NCUyQyUyMm9wdE91dCUyMiUzQWZhbHNlJTJDJTIybGFzdEV2ZW50VGltZSUyMiUzQTE3NDU0MTc0NTc4OTIlMkMlMjJsYXN0RXZlbnRJZCUyMiUzQTYzJTJDJTIycGFnZUNvdW50ZXIlMjIlM0ExMyU3RA==; " +
        "AMP_MKTG_572175c4a8=JTdCJTIycmVmZXJyZXIlMjIlM0ElMjJodHRwcyUzQSUyRiUyRnd3dy5nb29nbGUuY29tJTJGJTIyJTJDJTIycmVmZXJyaW5nX2RvbWFpbiUyMiUzQSUyMnd3dy5nb29nbGUuY29tJTIyJTdE; " +
        "amplitude_id_9f6c0bb8b82021496164c672a7dc98d6_edmbrown.edu=eyJkZXZpY2VJZCI6IjdiNGEzZmQ2LTdjMmUtNGZjZC1iMzUxLTRjNTg1OTI1ODE5M1IiLCJ1c2VySWQiOm51bGwsIm9wdE91dCI6ZmFsc2UsInNlc3Npb25JZCI6MTc0NTI2NDU2MTk4NSwibGFzdEV2ZW50VGltZSI6MTc0NTI2NDYwMzIzMSwiZXZlbnRJZCI6MCwiaWRlbnRpZnlJZCI6MjgsInNlcXVlbmNlTnVtYmVyIjoyOH0=";

    // Prepare request URL and body
    String url = "https://cab.brown.edu/api/?page=fose&route=search";
    String requestBody = String.format("{\"other\":{\"srcdb\":\"%s\"},\"criteria\":[{\"field\":\"is_ind_study\",\"value\":\"N\"},{\"field\":\"is_canc\",\"value\":\"N\"}]}", term);

    // Build the request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:137.0) Gecko/20100101 Firefox/137.0")
        .header("Accept", "application/json, text/javascript, */*; q=0.01")
        .header("Accept-Language", "en-US,en;q=0.5")
        .header("Content-Type", "application/json")
        .header("X-Requested-With", "XMLHttpRequest")
        .header("Origin", "https://cab.brown.edu")
        .header("Referer", "https://cab.brown.edu/")
        .header("Cookie", cookies)
        .POST(BodyPublishers.ofString(requestBody))
        .build();

    // Send the request
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

    // Save the raw JSON response to file
    Path outputPath = Paths.get(DATA_DIR, COURSES_FILE);
    Files.write(outputPath, response.body().getBytes());

    System.out.println("Successfully saved courses to: " + outputPath.toAbsolutePath());
    System.out.println("Number of courses fetched: " + response.body().split("\"code\":").length);
  }
}