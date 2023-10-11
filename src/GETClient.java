import com.google.gson.*;

import java.util.UUID;

public class GETClient {
  private NetworkHandler networkHandler;
  private final String serverID;
  private static final Gson gson = new Gson();
  private final LamportClock lamportClock;
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAUL_PORT = 4567;

  // Constructor to initialize the GETClient
  public GETClient(boolean isForTested) {
    this.networkHandler = new SocketNetworkHandler(isForTested);
    this.serverID = UUID.randomUUID().toString();
    this.lamportClock = new LamportClock();
  }

  public void interpretResponse(JsonObject response) {
    // check response
    if (response == null) {
      System.out.println("Error 400: No response from server.");
      return;
    }
    printWeatherData(response);
  }

  public NetworkHandler getNetworkHandler() {
    return this.networkHandler;
  }

  public LamportClock getLamportClock() {
    return this.lamportClock;
  }

  public void printWeatherData(JsonObject response) {
    try {
      String weatherDataText = JSONHandler.parseJSONtoText(response);
      for (String line : weatherDataText.split("\n")) {
        System.out.println(line);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error while converting JSON to text.", e);
    }
  }

  public JsonObject getData(String serverName, int port, String stationID) {
    int currentTime = lamportClock.send();
    networkHandler.initializeClientSocket(serverName, port);
    String getRequest = generateRequestString(currentTime, stationID);
    try {
      String response = networkHandler.receiveDataFromServer(serverName, port, getRequest);
      System.out.println("Response: " + getRequest);
      return handleServerResponse(response);
    } catch (Exception e) {
      System.out.println("Error 400: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  public String generateRequestString(int currentTime, String stationID) {
    return "GET /weather.json HTTP/1.1\r\n" +
        "ServerID: " + serverID + "\r\n" +
        "LamportClock: " + currentTime + "\r\n" +
        (stationID != null ? "StationID: " + stationID + "\r\n" : "") +
        "\r\n";
  }

  public JsonObject handleServerResponse(String responseStr) {
    if (responseStr.startsWith("500")) {
      System.out.println("Error 500: Incorrect format response");
      return null;
    }
    JsonObject jsonObject = gson.fromJson(JSONHandler.extractJSONContent(responseStr), JsonObject.class);
    if (jsonObject == null) {
      System.out.println("Error 400: No JSON object in response.");
      return null;
    }
    return jsonObject;
  }

  public static void main(String[] args) {

    String serverName;
    int port;
    String stationID = null;

    if (args.length >= 1) {
      String[] parts = args[0].split(":");

      if (parts.length == 2) {
        serverName = parts[0].replace("http://", "");
        port = Integer.parseInt(parts[1].split("/")[0]);

        if (args.length == 2) {
          stationID = args[1];
        }
      } else {
        System.err.println("Invalid argument format.");
        return;
      }
    } else {
      serverName = DEFAULT_HOST;
      port = DEFAUL_PORT;
    }

    // Initialize network handler and client
    GETClient client = new GETClient(false);

    // Get and interpret the data
    JsonObject response = client.getData(serverName, port, stationID);
    client.interpretResponse(response);
    client.networkHandler.closeResources();
  }
}
