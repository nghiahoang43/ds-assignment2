import org.json.JSONObject;

public class WeatherData implements Comparable<WeatherData> {
  private final JSONObject data; // Immutable reference to weather data
  private final int time; // Immutable Lamport clock time
  private final String serverId; // Immutable ID of the server from which the data is received

  public WeatherData(JSONObject data, int time, String serverId) {
    if (data == null || serverId == null) {
      throw new IllegalArgumentException("Data and server ID must not be null");
    }

    this.data = new JSONObject(data.toString()); // Deep copy to ensure immutability
    this.time = time;
    this.serverId = serverId;
  }

  public JSONObject getData() {
    return new JSONObject(data.toString()); // Return a deep copy to ensure immutability
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
