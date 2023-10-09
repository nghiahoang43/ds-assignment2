import org.junit.jupiter.api.*;
import com.google.gson.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class IntegrationTest {

  private ContentServer contentServer;
  private AggregationServer aggregationServer;
  private GETClient client;
  private int port = 4567;

  @BeforeEach
  public void setup() {
    // Set up necessary components before each test
    aggregationServer = new AggregationServer(false);
    new Thread(() -> {
      aggregationServer.start(port);
      try {
        Thread.sleep(1000); // Sleep for 1000 milliseconds after starting
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();
    contentServer = new ContentServer(false);
    client = new GETClient(false);
  }

  @AfterEach
  public void teardown() {
    // Clean up resources after each test
    aggregationServer.terminate();
    try {
      Thread.sleep(1000); // Sleep for 1000 milliseconds after starting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    client.getNetworkHandler().closeResources();
    contentServer.terminateResources();
    try {
      Thread.sleep(1000); // Sleep for 1000 milliseconds after starting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testGETClientToContentServer_validData() throws IOException, InterruptedException {
    String fileContent = JSONHandler.readFile("src/weather_test.txt");
    JsonObject parsedData = JSONHandler.parseTextToJSON(fileContent);
    contentServer.setWeatherData(parsedData);
    contentServer.uploadData("localhost", port);
    try {
      Thread.sleep(1000); // Sleep for 1000 milliseconds after starting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    JsonObject response = client.getData("localhost", port, "IDS60901");
    assertNotNull(response);
    assertEquals("IDS60901", response.get("id").getAsString());
  }

  @Test
  public void testGETClientToContentServer_malformedRequest() throws InterruptedException {
    JsonObject response = client.getData("localhost", port, null);

    assertNull(response);
    // Add more assertions based on expected error behavior.
  }

  @Test
  public void testContentServerToAggregationServer_dataTransfer() throws InterruptedException {
    contentServer.loadWeatherDataFromOutside("src/weather_test.txt");

    contentServer.uploadData("localhost", port);

    Thread.sleep(1000); // Ensure there's enough time for the data to be
    // transferred.

    // Assuming a method exists in AggregationServer to get the weather data for
    // a station.
    JsonObject data = aggregationServer.getWeatherData("IDS60901");

    assertNotNull(data);
    assertEquals("IDS60901", data.get("id").getAsString());
  }

  @Test
  public void testLamportClockWithAggregationServer() throws InterruptedException {
    int initialTime = aggregationServer.getLamportClockTime();

    client.getData("localhost", port, "testStationID");

    int newTime = aggregationServer.getLamportClockTime();
    assertTrue(newTime > initialTime);
  }

  @Test
  public void testAggregationServerDataOverwrite() throws InterruptedException, IOException {
    String fileContent = JSONHandler.readFile("src/weather_test.txt");
    JsonObject parsedData = JSONHandler.parseTextToJSON(fileContent);
    contentServer.setWeatherData(parsedData);
    contentServer.uploadData("localhost", port);
    try {
      Thread.sleep(1000); // Sleep for 1000 milliseconds after starting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Load new data and send again.
    fileContent = JSONHandler.readFile("src/weather_test_1.txt");
    parsedData = JSONHandler.parseTextToJSON(fileContent);
    contentServer.setWeatherData(parsedData);
    contentServer.uploadData("localhost", port);
    try {
      Thread.sleep(1000); // Sleep for 1000 milliseconds after starting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Thread.sleep(1000);

    JsonObject data = aggregationServer.getWeatherData("IDS60902");
    assertEquals("IDS60902", data.get("id").getAsString());
  }

  @Test
  public void testAggregationServerMultipleDataSources() throws InterruptedException, IOException {
    ContentServer anotherContentServer = new ContentServer(true);

    String fileContent = JSONHandler.readFile("src/weather_test.txt");
    JsonObject parsedData = JSONHandler.parseTextToJSON(fileContent);
    contentServer.setWeatherData(parsedData);
    contentServer.uploadData("localhost", port);
    try {
      Thread.sleep(1000); // Sleep for 1000 milliseconds after starting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    fileContent = JSONHandler.readFile("src/weather_test.txt");
    parsedData = JSONHandler.parseTextToJSON(fileContent);
    anotherContentServer.setWeatherData(parsedData);
    anotherContentServer.uploadData("localhost", port);
    try {
      Thread.sleep(1000); // Sleep for 1000 milliseconds after starting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Thread.sleep(1000);

    JsonObject data1 = aggregationServer.getWeatherData("IDS60901");
    JsonObject data2 = aggregationServer.getWeatherData("IDS60901");

    assertNotNull(data1);
    assertNotNull(data2);
  }

}
