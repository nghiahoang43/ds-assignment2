import com.google.gson.*;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentServer {
  private final NetworkHandler networkHandler;
  private JsonObject weatherData;
  private final ScheduledExecutorService dataUploadScheduler;
  private final String serverID;
  private final LamportClock lamportClock;
  private static final int DEFAUL_PORT = 4567;
  private static final String DEFAULT_HOST = "localhost";
  private static final String DEFAULT_PATH = "src/input.txt";

  public ContentServer(boolean isForTested) {
    this.serverID = UUID.randomUUID().toString();
    this.lamportClock = new LamportClock();
    this.dataUploadScheduler = Executors.newScheduledThreadPool(1);
    this.networkHandler = new SocketNetworkHandler(isForTested);
  }

  public NetworkHandler getNetworkHandler() {
    return this.networkHandler;
  }

  public LamportClock getLamportClock() {
    return this.lamportClock;
  }

  public void setWeatherData(JsonObject data) {
    this.weatherData = data;
  }

  public void uploadData(String host, int port) {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    executor.scheduleWithFixedDelay(new Runnable() {

      @Override
      public void run() {
        processPush(host, port);
      }
    }, 0, 30, TimeUnit.SECONDS);
  }

  public void processPush(String host, int port) {
    try {
      int serverLamportClock = networkHandler.initializeClientSocket(host, port);
      lamportClock.receive(serverLamportClock);
      int clockValue = openConnection(host, port);

      if (clockValue == -1) {
        System.out.println("Error 400: Bad Request No LamportClock");
        return;
      }
      adjustClock(clockValue);
      String request = buildRequest(host);
      String serverResponse = transceive(host, port, request);
      if (serverResponse != null) {
        processResponse(serverResponse);
      }

    } catch (Exception exc) {
      System.out.println("Connection issue: " + exc.getMessage());
      System.out.println("Retrying...");
      retryPush(host, port);
    }
  }

  private int openConnection(String host, int port) throws Exception {
    return networkHandler.initializeClientSocket(host, port);
  }

  public void adjustClock(int newTime) {
    lamportClock.receive(newTime);
  }

  public String buildRequest(String host) {
    String headers = String.format(
        "PUT /uploadData HTTP/1.1\r\nHost: %s\r\nServerID: %s\r\nLamportClock: %s\r\nContent-Type: application/json\r\nContent-Length: %d\r\n\r\n",
        host, serverID, lamportClock.getTime(), weatherData.toString().length());

    return headers + weatherData;
  }

  private String transceive(String host, int port, String data) throws Exception {
    return networkHandler.sendDataToServer(host, port, data);
  }

  public void processResponse(String res) {
    String[] headerLines = res.split("\r\n");
    for (String line : headerLines) {
      if (line.startsWith("LamportClock: ")) {
        lamportClock.receive(Integer.parseInt(line.split(": ")[1]));
        break;
      }
    }

    if (res.contains("200") || res.contains("201")) {
      System.out.println("\nData uploaded.");
    } else {
      System.out.println("Failed to push data. Server says: " + res);
    }
  }

  private void retryPush(String host, int port) {
    dataUploadScheduler.schedule(new Runnable() {
      @Override
      public void run() {
        processPush(host, port);
      }
    }, 15, TimeUnit.SECONDS);
  }

  public void terminateResources() {
    dataUploadScheduler.shutdown();
    networkHandler.closeResources();
  }

  public void loadWeatherDataFromOutside(String path) {
    System.out.println("Loading weather data from outside: " + path);
    try {
      String fileContent = JSONHandler.readFile(path);
      JsonObject parsedData = JSONHandler.parseTextToJSON(fileContent);
      this.setWeatherData(parsedData);
    } catch (Exception e) {
      System.out.println("Error 400: " + e.getMessage());
      return;
    }
  }

  public JsonObject getWeatherData() {
    return this.weatherData;
  }

  public static void main(String[] args) {
    try {
      String host = DEFAULT_HOST;
      int port = DEFAUL_PORT;
      String path = DEFAULT_PATH;
      if (args.length == 3) {
        host = args[0];
        port = Integer.parseInt(args[1]);
        path = args[2];
      } else if (args.length == 2) {
        host = args[0];
        port = Integer.parseInt(args[1]);
      } else if (args.length == 1) {
        host = args[0];
      }
      ContentServer server = new ContentServer(false);

      String fileContent = JSONHandler.readFile(path);
      JsonObject parsedData = JSONHandler.parseTextToJSON(fileContent);
      server.setWeatherData(parsedData);
      server.uploadData(host, port);

      // Get the current Runtime Environment
      Runtime runtime = Runtime.getRuntime();

      // Define what the new Thread will do
      Thread shutdownHook = new Thread(() -> {
        // This will be executed when the JVM shuts down
        server.terminateResources();
      });

      // Register the new Thread to run upon JVM shutdown
      runtime.addShutdownHook(shutdownHook);
    } catch (Exception e) {
      System.out.println("Error 400: " + e.getMessage());
      return;
    }
  }
}
