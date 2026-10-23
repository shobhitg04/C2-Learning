package tickets.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OllamaEmbedder implements Embedder {
  private final ObjectMapper json;
  private final String url;
  private final HttpClient http = HttpClient.newHttpClient();

  public OllamaEmbedder(ObjectMapper json, @Value("${app.ollama.url}") String url) {
    this.json = json;
    this.url = url;
  }

  @Override
  public float[] embed(String text) {
    try {
      var body = json.createObjectNode().put("model", "nomic-embed-text").put("prompt", text);
      var request = HttpRequest.newBuilder(URI.create(url + "/api/embeddings"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
          .build();
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      var values = json.readTree(response.body()).path("embedding");
      if (response.statusCode() != 200 || values.size() != 768) {
        throw new IllegalStateException("embedding failed");
      }
      var vector = new float[768];
      for (int i = 0; i < 768; i++) {
        vector[i] = (float) values.get(i).asDouble();
      }
      return vector;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("embedding failed", ex);
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("embedding failed", ex);
    }
  }
}
