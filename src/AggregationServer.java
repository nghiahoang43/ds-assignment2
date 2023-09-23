import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class AggregationServer {

  private volatile boolean shutdown = false;
  private NetworkHandler networkHandler;

  // Lamport clock for the server
  private final LamportClock lamportClock = new LamportClock();

  // To store JSON entries along with their sources (thread-safe)
  private final Map<String, PriorityQueue<WeatherData>> dataStore = new ConcurrentHashMap<>();

  // Thread-safe blocking queue for incoming requests
  private final BlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();

  public AggregationServer(NetworkHandler networkHandler) {
    this.networkHandler = networkHandler;
  }

  // Monitor for shutdown commands
  private void initializeShutdownMonitor() {
    new Thread(() -> {
      Scanner scanner = new Scanner(System.in);
      while (true) {
        String input = scanner.nextLine();
        if ("SHUTDOWN".equalsIgnoreCase(input)) {
          shutdown();
          break;
        }
      }
    }).start();
  }

  // Accepts incoming client connections
  private void initializeAcceptThread() {
    new Thread(() -> {
      while (!shutdown) {
        Socket clientSocket = networkHandler.acceptIncomingClient();
        requestQueue.offer(clientSocket);
      }
    }).start();
  }

  // Main loop for handling client requests
  private void processClientRequests() {
    try {
      while (!shutdown) {
        Socket clientSocket = requestQueue.take();
        handleClientSocket(clientSocket);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    } finally {
      networkHandler.closeResources();
    }
  }

  // Handles individual client socket
  private void handleClientSocket(Socket clientSocket) {
    try {
      String requestData = networkHandler.waitForDataFromClient(clientSocket);
      if (requestData != null) {
        String responseData = handleRequest(requestData);
        networkHandler.sendResponseToClient(responseData, clientSocket);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        clientSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void start(int portNumber) {
    networkHandler.initializeServer(portNumber);
    initializeShutdownMonitor();
    initializeAcceptThread();
    processClientRequests();
  }

  public void shutdown() {
    this.shutdown = true;
    System.out.println("Shutting down server...");
  }

  // Extracts information from raw request data and calls the corresponding
  // handler
  public String handleRequest(String requestData) {
    String[] lines = requestData.split("\r\n");
    String requestType = lines[0].split(" ")[0].trim();

    Map<String, String> headers = new HashMap<>();
    StringBuilder contentBuilder = new StringBuilder();

    boolean readingContent = false;

    for (int i = 1; i < lines.length; i++) {
      if (!readingContent) {
        if (lines[i].isEmpty()) {
          readingContent = true;
        } else {
          String[] headerParts = lines[i].split(": ", 2);
          headers.put(headerParts[0], headerParts[1]);
        }
      } else {
        contentBuilder.append(lines[i]);
      }
    }

    String content = contentBuilder.toString();

    if ("GET".equalsIgnoreCase(requestType)) {
      return handleGetRequest(headers, content);
    } else if ("PUT".equalsIgnoreCase(requestType)) {
      return handlePutRequest(headers, content);
    } else {
      return "Error 400: Bad Request";
    }
  }

  // Handles GET requests
  private String handleGetRequest(Map<String, String> headers, String content) {
    int lamportTime = Integer.parseInt(headers.getOrDefault("LamportClock", "-1"));
    lamportClock.receive(lamportTime);

    String stationId = headers.get("StationID");
    if (stationId == null || stationId.isEmpty()) {
      // If no station ID is provided, get the first station's data from the datastore
      Optional<String> optionalStationId = dataStore.keySet().stream().findFirst();
      System.out.println("optionalStationId: " + optionalStationId);
      if (optionalStationId.isPresent()) {
        stationId = optionalStationId.get();
      } else {
        return "404 Not Found Null StationID";
      }
    }

    PriorityQueue<WeatherData> weatherDataQueue = dataStore.get(stationId);
    if (weatherDataQueue == null || weatherDataQueue.isEmpty()) {
      return "404 Not Found"; // No data available for the given station ID
    }

    // Find the first WeatherData with Lamport time less than the request's Lamport
    // time
    Optional<WeatherData> targetData = weatherDataQueue.stream()
        .filter(data -> data.getTime() <= lamportTime)
        .findFirst();

    if (targetData.isEmpty()) {
      return "404 Not Found No Valid Data"; // No data available matching the Lamport time condition
    }

    return "Station ID: " + stationId + "\n" + targetData.get();

  }

  // Handles PUT requests
  private String handlePutRequest(Map<String, String> headers, String content) {
    int lamportTime = Integer.parseInt(headers.getOrDefault("LamportClock", "-1"));
    lamportClock.receive(lamportTime);

    // Extract the server ID
    String serverId = headers.get("ServerID");
    System.out.println(headers);
    if (serverId == null || serverId.isEmpty()) {
      return "Error 400: Bad Request No ServerID"; // Server ID is mandatory in PUT request
    }

    // Parse the content into a JSONObject
    JSONObject weatherDataJSON;
    try {
      weatherDataJSON = new JSONObject(content);
    } catch (Exception e) {
      return "Error 400: Bad Request Malformed JSON"; // Malformed JSON data
    }

    // Use the new method to add weather data to the DataStore
    if (addWeatherData(weatherDataJSON, lamportTime, serverId)) {
      return "200: OK";
    } else {
      return "Error 400: Bad Request"; // Failed to add data
    }
  }

  // Adds weather data to the DataStore
  public boolean addWeatherData(JSONObject weatherDataJSON, int lamportTime, String serverId) {
    String stationId = weatherDataJSON.optString("id", null);
    if (stationId == null || stationId.isEmpty()) {
      return false;
    }

    WeatherData newData = new WeatherData(weatherDataJSON, lamportTime, serverId);
    dataStore.computeIfAbsent(stationId, k -> new PriorityQueue<>()).offer(newData);

    return true;
  }

  public static void main(String[] args) {
    int port = Integer.parseInt(args[0]);

    NetworkHandler networkHandler = new SocketNetworkHandler();
    AggregationServer server = new AggregationServer(networkHandler);
    server.start(port);
  }
}
