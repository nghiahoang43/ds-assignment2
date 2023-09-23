import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentServer {
  private final String serverID;
  private final LamportClock lamportClock;
  private JSONObject weatherData;
  private final ScheduledExecutorService dataUploadScheduler;
  private final NetworkHandler networkHandler;

  public ContentServer(NetworkHandler networkHandler) {
    this.serverID = UUID.randomUUID().toString();
    this.lamportClock = new LamportClock();
    this.dataUploadScheduler = Executors.newScheduledThreadPool(1);
    this.networkHandler = networkHandler;
  }

  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println("Usage: ContentServer <serverName> <portNumber> <filePath>");
      return;
    }

    String serverName = args[0];
    int portNumber;
    try {
      portNumber = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      System.out.println("Error 400: Invalid port number provided.");
      return;
    }
    String filePath = args[2];

    NetworkHandler networkHandler = new SocketNetworkHandler();
    ContentServer server = new ContentServer(networkHandler);

    if (!server.loadWeatherData(filePath)) {
      System.out.println("Error 400: Failed to load weather data from " + filePath);
      return;
    }

    server.uploadWeatherData(serverName, portNumber);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      server.terminateResources();
    }));
  }

  public boolean loadWeatherData(String filePath) {
    try {
      String fileContent = JSONHandler.readFile(filePath);
      weatherData = JSONHandler.parseTextToJSON(fileContent);
      return true;
    } catch (Exception e) {
      System.out.println("Error loading weather data: " + e.getMessage());
      return false;
    }
  }

  public void uploadWeatherData(String serverName, int portNumber) {
    dataUploadScheduler.scheduleAtFixedRate(() -> {
      try {
        JSONObject jsonData = new JSONObject(weatherData.toString());
        jsonData.put("LamportClock", lamportClock.send());
        System.out.println("Uploading data to server...");
        String data = createPutRequest(jsonData, serverName);
        System.out.println(data);
        String response = networkHandler.sendDataToServer(serverName, portNumber, data);
        if (response != null && (response.contains("200") || response.contains("201"))) {
          System.out.println("Data uploaded successfully.");
        } else {
          System.out.println("Error uploading data");
        }
      } catch (Exception e) {
        System.out.println("Error while connecting to the server: " + e.getMessage());
      }
    }, 0, 30, TimeUnit.SECONDS);
  }

  private String createPutRequest(JSONObject jsonData, String serverName) {
    return "PUT /uploadData HTTP/1.1\r\n" +
        "Host: " + serverName + "\r\n" +
        "ServerID: " + serverID + "\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: " + jsonData.toString().length() + "\r\n" +
        "\r\n" +
        jsonData;
  }

  public void terminateResources() {
    dataUploadScheduler.shutdown();
    networkHandler.closeResources();
  }
}
