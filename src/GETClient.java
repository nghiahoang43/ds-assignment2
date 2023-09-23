import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.UUID;

public class GETClient {
  private final NetworkHandler networkHandler;
  private final String serverId;
  private final LamportClock lamportClock;

  // Constructor to initialize the GETClient
  public GETClient(NetworkHandler networkHandler) {
    this.networkHandler = networkHandler;
    this.serverId = UUID.randomUUID().toString();
    this.lamportClock = new LamportClock();
  }

  public void interpretResponse(JSONObject response) {
    // check response
    if (response == null) {
      System.out.println("Error 400: No response from server.");
      return;
    }

    // check status
    String status = response.optString("status", null);

    if (status == null) {
      System.out.println("Error 400: Invalid response format.");
      return;
    }

    // check LamportClock
    switch (status) {
      case "not available":
        System.out.println("Error 400: No weather data available.");
        break;
      case "available":
        printWeatherData(response);
        break;
      default:
        System.out.println("Error 400: Unknown response status: " + status);
    }
  }

  private void printWeatherData(JSONObject response) {
    try {
      String weatherDataText = JSONHandler.parseJSONtoText(response);
      for (String line : weatherDataText.split("\n")) {
        System.out.println(line);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error while converting JSON to text.", e);
    }
  }

  public JSONObject getData(String serverName, int port, String stationID) {
    int currentTime = lamportClock.send();
    String getRequest = generateRequestString(currentTime, stationID);

    try {
      String response = networkHandler.receiveDataFromServer(serverName, port, getRequest);
      System.out.println("Response from server: " + response);
      return handleServerResponse(response);
    } catch (Exception e) {
      System.out.println("Error 400: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  private String generateRequestString(int currentTime, String stationID) {
    return "GET /weather.json HTTP/1.1\r\n" +
        "serverId: " + serverId + "\r\n" +
        "LamportClock: " + currentTime + "\r\n" +
        (stationID != null ? "StationID: " + stationID + "\r\n" : "") +
        "\r\n";
  }

  private JSONObject handleServerResponse(String responseStr) throws JSONException {
    if (responseStr.startsWith("500")) {
      System.out.println("Error 500: Incorrect format response");
      return null;
    }

    JSONObject jsonObject = new JSONObject(new JSONTokener(responseStr));
    if (jsonObject.has("LamportClock")) {
      lamportClock.receive(jsonObject.getInt("LamportClock"));
    }

    return jsonObject;
  }

  public static void main(String[] args) {
    // Validate and parse command-line arguments
    if (args.length < 2) {
      System.out.println("Usage: GETClient <serverName>:<port> [<stationID>]");
      return;
    }

    String serverName = args[0].split(":")[0];
    int port = Integer.parseInt(args[0].split(":")[1]);
    String stationID = args.length == 3 ? args[2] : null;

    // Initialize network handler and client
    NetworkHandler networkHandler = new SocketNetworkHandler();
    GETClient client = new GETClient(networkHandler);

    // Get and interpret the data
    JSONObject response = client.getData(serverName, port, stationID);
    client.interpretResponse(response);
  }
}
