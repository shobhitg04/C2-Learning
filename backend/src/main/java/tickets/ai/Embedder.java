package tickets.ai;

public interface Embedder {
  float[] embed(String text);
}
