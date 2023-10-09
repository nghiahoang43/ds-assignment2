import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

public class ContentServerTest {

  @Mock
  private NetworkHandler networkHandler;

  @Mock
  private LamportClock lamportClock;

  private ContentServer server;

  @BeforeEach
  public void initialize() {
    MockitoAnnotations.initMocks(this);
    server = new ContentServer(true);
    networkHandler = server.getNetworkHandler();
    lamportClock = server.getLamportClock();
  }

  @Test
  public void testSetWeatherData() {
    JsonObject fakeData = new JsonObject();
    fakeData.addProperty("temperature", "20C");
    server.setWeatherData(fakeData);
    assertEquals(fakeData, server.getWeatherData());
  }

  @Test
  public void testAdjustClock() {
    server.adjustClock(5);
    assertEquals(6, server.getLamportClock().getTime());
  }

  @Test
  public void testBuildRequest() {
    String host = "localhost";
    int port = 4567;
    String path = "src/weather_test.txt";

    server.loadWeatherDataFromOutside(path);
    server.uploadData(host, port);
    server.getLamportClock().send();
    assertEquals(1, server.getLamportClock().getTime());
  }

  @Test
  public void testProcessResponse_withLamportClock() {
    server.processResponse("LamportClock: 10\r\nHTTP/1.1 200 OK");
    assertEquals(11, server.getLamportClock().getTime());
  }

  @Test
  public void testProcessResponse_dataUploaded() {
    server.processResponse("HTTP/1.1 200 OK");
    // Check if "Data uploaded." was printed.
  }

  @Test
  public void testProcessResponse_failedToPushData() {
    server.processResponse("HTTP/1.1 404 Not Found");
    // Check if "Failed to push data." was printed.
  }

  @Test
  public void testLoadWeatherDataFromOutside() {
    String path = "src/weather_test_wrong.txt";

    server.loadWeatherDataFromOutside(path);
    JsonObject weatherData = server.getWeatherData();
    assertEquals(weatherData, null);
  }

  @Test
  public void testTerminateResources() {
    server.terminateResources();
    assertTrue(networkHandler.checkClientSocketIsClosed());
  }

  @Test
  public void testGetNetworkHandler() {
    assertEquals(networkHandler, server.getNetworkHandler());
  }

  @Test
  void testUploadWeatherDataWithoutSocket() {
    String host = "localhost";
    int port = 4567;
    String path = "src/weather_test.txt";

    server.loadWeatherDataFromOutside(path);
    server.uploadData(host, port);

    // sleep for 10 seconds
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    JsonObject weatherData = server.getWeatherData();
    assertEquals(weatherData.get("id").getAsString(), "IDS60901");
  }

  @Test
  void testUploadWeatherDataFromOutsideWithWrongPath() {
    String path = "src/weather_test_wrong.txt";

    server.loadWeatherDataFromOutside(path);
    JsonObject weatherData = server.getWeatherData();
    assertEquals(weatherData, null);
  }

  @Test
  void testUploadWeatherDataFromOutsideithWrongFormat() {
    String path = "src/weather_test_wrong_format.txt";

    server.loadWeatherDataFromOutside(path);
    JsonObject weatherData = server.getWeatherData();
    assertEquals(weatherData, null);
  }
}
