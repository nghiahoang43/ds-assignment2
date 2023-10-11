import com.google.gson.*;

public class WeatherData implements Comparable<WeatherData> {
  private final int time;
  private final JsonObject data;
  private final String serverID;

  public WeatherData(JsonObject data, int time, String serverID) {
    if (data == null || serverID == null) {
      throw new IllegalArgumentException("Error 400: data or serverID is null.");
    }

    this.time = time;
    this.data = data;
    this.serverID = serverID;
  }

  public JsonObject getData() {
    return data;
  }

  public int getTime() {
    return time;
  }

  public String getserverID() {
    return serverID;
  }

  @Override
  public int compareTo(WeatherData other) {
    return Integer.compare(this.time, other.time);
  }

  @Override
  public String toString() {
    return "LamportTime: " + time + ", serverID: " + serverID + ", Data: " + data.toString();
  }
}
