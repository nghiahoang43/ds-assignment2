import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationServerTest {

  private AggregationServer aggregationServer;

  @BeforeEach
  public void setUp() {
    aggregationServer = new AggregationServer(true);
  }

  @Test
  public void testAddWeatherData_validData() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("id", "station1");
    jsonObject.addProperty("temp", 20.5);
    assertTrue(aggregationServer.addWeatherData(jsonObject, 1, "server1"));
  }

  @Test
  public void testAddWeatherData_missingID() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("temp", 20.5);
    assertFalse(aggregationServer.addWeatherData(jsonObject, 1, "server1"));
  }

  @Test
  public void testAddWeatherData_emptyID() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("id", "");
    jsonObject.addProperty("temp", 20.5);
    assertFalse(aggregationServer.addWeatherData(jsonObject, 1, "server1"));
  }

  @Test
  public void testProcessRequest_getRequestWithValidData() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("id", "station1");
    jsonObject.addProperty("temp", 20.5);
    aggregationServer.addWeatherData(jsonObject, 1, "server1");

    String getRequest = "GET /weatherData HTTP/1.1\r\nStationID: station1\r\nLamportClock: 1\r\n\r\n";
    String response = aggregationServer.processRequest(getRequest);
    assertTrue(response.contains("200 OK"));
  }

  @Test
  public void testProcessRequest_getRequestWithoutData() {
    String getRequest = "GET /weatherData HTTP/1.1\r\nStationID: unknown\r\nLamportClock: 1\r\n\r\n";
    String response = aggregationServer.processRequest(getRequest);
    assertTrue(response.contains("204 No Content"));
  }

  @Test
  public void testProcessRequest_firstPutRequestWithValidData() {
    String putRequest = "PUT /weatherData HTTP/1.1\r\nServerID: server1\r\nLamportClock: 1\r\n\r\n{id:\"station1\", temp:20.5}";
    String response = aggregationServer.processRequest(putRequest);
    assertTrue(response.contains("201 HTTP_CREATED"));
  }

  @Test
  public void testProcessRequest_secondPutRequestWithValidData() {
    String putRequest = "PUT /weatherData HTTP/1.1\r\nServerID: server1\r\nLamportClock: 1\r\n\r\n{id:\"station1\", temp:20.5}";
    String response = aggregationServer.processRequest(putRequest);
    response = aggregationServer.processRequest(putRequest);
    assertTrue(response.contains("200 OK"));
  }

  @Test
  public void testProcessRequest_invalidRequest() {
    String invalidRequest = "POST /weatherData HTTP/1.1\r\nServerID: server1\r\nLamportClock: 1\r\n\r\n{id:\"station1\", temp:20.5}";
    String response = aggregationServer.processRequest(invalidRequest);
    assertTrue(response.contains("400 Bad Request"));
  }

  @Test
  public void testTerminate() {
    aggregationServer.terminate();
    assertTrue(aggregationServer.getShutdownFlag());
  }

  @Test
  public void testExtractID_validData() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("id", "station1");
    assertEquals("station1", aggregationServer.extractID(jsonObject));
  }

  @Test
  public void testExtractID_noID() {
    JsonObject jsonObject = new JsonObject();
    assertNull(aggregationServer.extractID(jsonObject));
  }

  @Test
  public void testIsValidStation_validData() {
    assertTrue(aggregationServer.isValidStation("station1"));
  }

  @Test
  public void testIsValidStation_emptyData() {
    assertFalse(aggregationServer.isValidStation(""));
  }

  @Test
  public void testIsValidStation_nullData() {
    assertFalse(aggregationServer.isValidStation(null));
  }

  @Test
  public void testShutdownFlag_initialState() {
    assertFalse(aggregationServer.getShutdownFlag());
  }

  @Test
  public void testNetworkHandler_instanceNotNull() {
    assertNotNull(aggregationServer.getNetworkHandler());
  }
}
