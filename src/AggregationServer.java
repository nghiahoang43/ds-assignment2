import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Type;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class AggregationServer {
  private Map<String, PriorityQueue<WeatherData>> weatherDataMap = new ConcurrentHashMap<>();
  private Map<String, Long> timeMap = new ConcurrentHashMap<>();
  private LinkedBlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();
  private LamportClock lamportClock = new LamportClock();
  private static final Gson gson = new Gson();
  private static final String DATA_FILE_PATH = "src" + File.separator + "data.json";
  private static final String TIME_FILE = "src" + File.separator + "timeData.json";
  private static final String INIT_DATA_FILE = "src" + File.separator + "initData.json";
  private static final String INIT_TIME_FILE = "src" + File.separator + "initTimeData.json";
  private Thread acceptThread;
  private ScheduledExecutorService fileSaveScheduler;
  private ScheduledExecutorService cleanupScheduler;
  private volatile boolean shutdownFlag = false;
  private static final int DEFAUL_PORT = 4567;
  private NetworkHandler networkHandler;

  public static void main(String[] args) {
    int port;
    if (args.length == 0) {
      port = DEFAUL_PORT;
    } else {
      port = Integer.parseInt(args[0]);
    }
    AggregationServer server = new AggregationServer(false);
    server.start(port);
  }

  public AggregationServer(boolean isForTested) {
    this.networkHandler = new SocketNetworkHandler(isForTested);
  }

  public void start(int portNumber) {
    System.out.println("server start");
    networkHandler.initializeServer(portNumber);

    fileSaveScheduler = Executors.newScheduledThreadPool(1);
    fileSaveScheduler.scheduleAtFixedRate(this::saveDataToFile, 0, 60, TimeUnit.SECONDS);

    loadDataFromFile();

    cleanupScheduler = Executors.newScheduledThreadPool(1);
    cleanupScheduler.scheduleAtFixedRate(this::cleanupStaleEntries, 0, 21, TimeUnit.SECONDS);

    initializeAcceptThread();

    processClientRequests();
  }

  private synchronized void saveDataToFile() {
    saveObjectToFile(weatherDataMap, DATA_FILE_PATH, INIT_DATA_FILE);
    saveObjectToFile(weatherDataMap, TIME_FILE, INIT_TIME_FILE);
  }

  private synchronized void saveObjectToFile(
      Map<String, PriorityQueue<WeatherData>> object,
      String filePath,
      String initFile) {
    try {
      String jsonData = gson.toJson(object);

      Files.write(Paths.get(initFile), jsonData.getBytes());

      Files.move(Paths.get(initFile),
          Paths.get(filePath),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public JsonObject getWeatherData(String stationID) {
    PriorityQueue<WeatherData> queue = weatherDataMap.get(stationID);
    if (queue == null || queue.isEmpty()) {
      return null;
    }
    return queue.peek().getData();
  }

  public int getLamportClockTime() {
    return lamportClock.getTime();
  }

  public void loadDataFromFile() {
    Map<String, PriorityQueue<WeatherData>> loadedQueue = readDataFile(
        DATA_FILE_PATH,
        INIT_DATA_FILE,
        new TypeToken<ConcurrentHashMap<String, PriorityQueue<WeatherData>>>() {
        }.getType());

    Map<String, Long> loadedTimeService = readDataFile(
        TIME_FILE,
        INIT_TIME_FILE,
        new TypeToken<ConcurrentHashMap<String, Long>>() {
        }.getType());

    this.weatherDataMap = loadedQueue;
    this.timeMap = loadedTimeService;
  }

  private <T> T readDataFile(String file, String initFile, Type type) {
    try {
      String jsonData = new String(Files.readAllBytes(Paths.get(file)));
      return gson.fromJson(jsonData, type);
    } catch (IOException e) {
      try {
        String data = new String(Files.readAllBytes(Paths.get(initFile)));
        return gson.fromJson(data, type);
      } catch (IOException ex) {
        ex.printStackTrace();
        return null;
      }
    }
  }

  private void cleanupStaleEntries() {
    long currentTime = System.currentTimeMillis();

    // Identify stale server IDs
    Set<String> staleServerIDs = timeMap.keySet().stream()
        .filter(entry -> currentTime - timeMap.get(entry) > 20000)
        .collect(Collectors.toSet());

    staleServerIDs.forEach(timeMap::remove);

    if (timeMap.keySet().isEmpty()) {
      weatherDataMap.keySet().forEach(weatherDataMap::remove);
      return;
    }

    for (String stationID : weatherDataMap.keySet()) {
      PriorityQueue<WeatherData> queue = weatherDataMap.get(stationID);
      queue.removeIf(weatherData -> staleServerIDs.contains(weatherData.getServerId()));

      if (queue.isEmpty()) {
        weatherDataMap.remove(stationID);
      }
    }
  }

  private void initializeAcceptThread() {
    System.out.println("Initializing accept thread...\n");
    acceptThread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Socket clientSocket = networkHandler.acceptIncomingClient();
          if (clientSocket != null) {
            String clockValue = "LamportClock: " + lamportClock.getTime() + "\r\n";
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println(clockValue);
            out.flush();

            requestQueue.put(clientSocket);
            System.out.println("Added connection to request queue\n");
          }
        } catch (IOException e) {
          if (Thread.currentThread().isInterrupted()) {
            break;
          }
          e.printStackTrace();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    });
    acceptThread.start();
  }

  private void processClientRequests() {
    System.out.println("Processing client requests...\n");
    try {
      while (!shutdownFlag) {
        Socket clientSocket = waitForClient();
        if (clientSocket != null) {
          System.out.println("New connection\n");
          handleClientSocket(clientSocket);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      networkHandler.closeResources();
    }
  }

  private Socket waitForClient() throws InterruptedException {
    if (shutdownFlag)
      return null;
    return requestQueue.poll(10, TimeUnit.MILLISECONDS);
  }

  private void handleClientSocket(Socket clientSocket) {
    try {
      String requestData = networkHandler.waitForDataFromClient(clientSocket);
      if (requestData != null) {
        String responseData = processRequest(requestData);
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

  public String processRequest(String inputData) {
    List<String> lineList = Arrays.asList(inputData.split("\r\n"));
    String reqMethod = lineList.get(0).split(" ")[0];

    Map<String, String> headerMap = extractHeaders(lineList);
    String bodyContent = extractContent(lineList);
    switch (reqMethod.toUpperCase()) {
      case "GET":
        return processGet(headerMap, bodyContent);
      case "PUT":
        return processPut(headerMap, bodyContent);
      default:
        return constructResponse("400 Bad Request", null);
    }
  }

  private Map<String, String> extractHeaders(List<String> lineList) {
    Map<String, String> headerMap = new HashMap<>();
    for (String line : lineList) {
      if (line.contains(": ")) {
        String[] parts = line.split(": ", 2);
        headerMap.put(parts[0], parts[1]);
      }
    }
    return headerMap;
  }

  private String extractContent(List<String> lineList) {
    int startIndex = lineList.indexOf("") + 1;
    return String.join("", lineList.subList(startIndex, lineList.size()));
  }

  private String processGet(Map<String, String> headers, String content) {
    int lamportTimestamp = extractLamportTime(headers);
    String stationKey = findStationId(headers);
    if (stationKey == null) {
      return constructResponse("204 No Content", null);
    }
    PriorityQueue<WeatherData> dataQueue = weatherDataMap.get(stationKey);
    if (isQueueEmpty(dataQueue)) {
      return constructResponse("204 No Content", null);
    }

    Optional<WeatherData> matchingData = locateWeatherData(dataQueue, lamportTimestamp);
    return matchingData.map(data -> constructResponse("200 OK", data.getData().toString()))
        .orElse(constructResponse("204 No Content", null));
  }

  private String processPut(Map<String, String> headers, String content) {
    int lamportTimestamp = extractLamportTime(headers);
    String serverKey = headers.get("ServerID");
    if (isValidSource(serverKey) && processData(content, lamportTimestamp, serverKey)) {
      return generateResponse(serverKey);
    } else {
      return constructResponse("400 Bad Request", null);
    }
  }

  private String constructResponse(String status, String json) {
    StringBuilder result = new StringBuilder();

    result.append("HTTP/1.1 ").append(status).append("\r\n");
    result.append("LamportClock: ").append(lamportClock.send()).append("\r\n");

    if (json != null) {
      result.append("Content-Type: application/json\r\n");
      result.append("Content-Length: ").append(json.length()).append("\r\n\r\n");
      result.append(json);
    }
    return result.toString();
  }

  private int extractLamportTime(Map<String, String> headers) {
    int lamportTime = Integer.parseInt(headers.getOrDefault("LamportClock", "-1"));
    lamportClock.receive(lamportTime);
    return lamportClock.getTime();
  }

  private String findStationId(Map<String, String> headers) {
    String stationId = headers.get("StationID");
    if (stationId != null && !stationId.isEmpty()) {
      return stationId;
    }
    return weatherDataMap.keySet().stream().findFirst().orElse(null);
  }

  private boolean isQueueEmpty(PriorityQueue<WeatherData> queue) {
    return queue == null || queue.isEmpty();
  }

  private Optional<WeatherData> locateWeatherData(PriorityQueue<WeatherData> queue, int lamportTime) {
    return queue.stream()
        .filter(data -> data.getTime() <= lamportTime)
        .findFirst();
  }

  private boolean isValidSource(String serverID) {
    return serverID != null && !serverID.isEmpty();
  }

  private boolean processData(String content, int lamportTime, String serverID) {
    try {
      JsonObject weatherDataJSON = gson.fromJson(content, JsonObject.class);
      String stationID = extractID(weatherDataJSON);
      WeatherData newWeatherData = new WeatherData(weatherDataJSON, lamportTime, serverID);
      weatherDataMap.computeIfAbsent(stationID, k -> new PriorityQueue<>()).add(newWeatherData);
      return true;
    } catch (JsonParseException e) {
      System.err.println("JSON Parsing Error: " + e.getMessage());
      return false;
    }
  }

  private String generateResponse(String serverID) {
    long currentTimestamp = System.currentTimeMillis();
    Long lastTimestamp = timeMap.get(serverID);
    timeMap.put(serverID, currentTimestamp);

    if (isNewOrDelayedRequest(lastTimestamp, currentTimestamp)) {
      return constructResponse("201 HTTP_CREATED", null);
    } else {
      return constructResponse("200 OK", null);
    }
  }

  private boolean isNewOrDelayedRequest(Long lastTimestamp, long currentTimestamp) {
    return lastTimestamp == null || (currentTimestamp - lastTimestamp) > 20000;
  }

  public boolean addWeatherData(JsonObject weatherDataJSON, int lamportTime, String serverID) {
    String id = extractID(weatherDataJSON);

    if (!isValidStation(id)) {
      return false;
    }
    weatherDataMap.computeIfAbsent(id, k -> new PriorityQueue<>())
        .add(new WeatherData(weatherDataJSON, lamportTime, serverID));
    return true;
  }

  public NetworkHandler getNetworkHandler() {
    return this.networkHandler;
  }

  public String extractID(JsonObject weatherDataJSON) {
    return weatherDataJSON.has("id") ? weatherDataJSON.get("id").toString().replace("\"", "") : null;
  }

  public boolean isValidStation(String stationId) {
    return stationId != null && !stationId.isEmpty();
  }

  public void terminate() {
    markShutdown();

    haltThread(acceptThread);
    stopScheduledTask(fileSaveScheduler, 5);
    stopScheduledTask(cleanupScheduler, 60);

    System.out.println("Server termination initiated...");
  }

  private void markShutdown() {
    this.shutdownFlag = true;
  }

  public boolean getShutdownFlag() {
    return this.shutdownFlag;
  }

  private void haltThread(Thread t) {
    if (t != null) {
      t.interrupt();
    }
  }

  private void stopScheduledTask(ExecutorService scheduler, int timeoutSeconds) {
    if (scheduler == null) {
      return;
    }

    scheduler.shutdown();

    try {
      if (!scheduler.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException ex) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
