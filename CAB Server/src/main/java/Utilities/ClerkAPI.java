package Utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ClerkAPI {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String CLERK_SECRET_KEY =
      "sk_test_aYPrgZ2mZYpoTMeUSZqeDXlztrkweExpiFTZQ4UVGx";
  private static final String CLERK_API_URL = "https://api.clerk.dev/v1/users/";

  public static JsonNode getUser(String userId) throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request =
        new Request.Builder()
            .url(CLERK_API_URL + userId)
            .addHeader("Authorization", "Bearer " + CLERK_SECRET_KEY)
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new Exception("Clerk API Error: " + response.code());
      }
      return mapper.readTree(response.body().string());
    }
  }
}
