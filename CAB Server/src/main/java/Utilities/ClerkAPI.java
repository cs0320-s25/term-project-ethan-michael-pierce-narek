package Utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Client for interacting with the Clerk authentication API. This utility provides methods to
 * retrieve user information from Clerk, which is used for authentication and user metadata storage
 * in the course scheduling application.
 */
public class ClerkAPI {

  /** JSON object mapper for parsing API responses */
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Clerk API secret key for server-side authentication. Note: In a production environment, this
   * should be stored in environment variables or a secure configuration system, not hardcoded.
   */
  private static final String CLERK_SECRET_KEY =
      "sk_test_aYPrgZ2mZYpoTMeUSZqeDXlztrkweExpiFTZQ4UVGx";

  /** Base URL for Clerk API requests */
  private static final String CLERK_API_URL = "https://api.clerk.dev/v1/users/";

  /**
   * Retrieves detailed information about a Clerk user. This method makes an authenticated request
   * to the Clerk API to fetch user profile and metadata for the specified user ID.
   *
   * @param userId The Clerk user ID to retrieve information for
   * @return A JsonNode containing the user's profile information and metadata
   * @throws Exception If the API request fails or returns an error
   */
  public static JsonNode getUser(String userId) throws Exception {
    // Create HTTP client
    OkHttpClient client = new OkHttpClient();

    // Build request with authorization header
    Request request =
        new Request.Builder()
            .url(CLERK_API_URL + userId)
            .addHeader("Authorization", "Bearer " + CLERK_SECRET_KEY)
            .build();

    // Execute request and handle response
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new Exception("Clerk API Error: " + response.code());
      }
      // Parse response body to JSON
      return mapper.readTree(response.body().string());
    }
  }
}
