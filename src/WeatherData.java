import com.google.gson.*;

public class WeatherData implements Comparable<WeatherData> {
  private final JsonObject data; // Immutable reference to weather data
  private final int time; // Immutable Lamport clock time
  private final String serverId; // Immutable ID of the server from which the data is received

  public WeatherData(JsonObject data, int time, String serverId) {
    if (data == null || serverId == null) {
      throw new IllegalArgumentException("Data and server ID must not be null");
    }

    this.data = data; // Deep copy to ensure immutability
    this.time = time;
    this.serverId = serverId;
  }

  public JsonObject getData() {
    return data; // Return a deep copy to ensure immutability
  }

  public String getServerId() {
    return serverId;
  }

  public int getTime() {
    return time;
  }

  @Override
  public int compareTo(WeatherData other) {
    return Integer.compare(this.time, other.time);
  }

  @Override
  public String toString() {
    return "LamportTime: " + time + ", ServerID: " + serverId + ", Data: " + data.toString();
  }
}
