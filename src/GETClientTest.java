// import static org.junit.Assert.assertEquals;

// import org.junit.jupiter.api.*;

// import com.google.gson.*;

// public class GETClientTest {
//   GETClient client;
//   NetworkHandler networkHandler;

//   @BeforeEach
//   void initialize() {
//     client = new GETClient(true);
//     networkHandler = client.getNetworkHandler();
//   }

//   @Test


// }
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;

public class GETClientTest {
  
    @Mock
    private NetworkHandler networkHandler;
  
    @Mock
    private LamportClock lamportClock;
  
    private GETClient getClient;
  
    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        getClient = new GETClient(true);
        networkHandler = getClient.getNetworkHandler();
    }

    @Test
    public void testInterpretResponse_nullResponse() {
        getClient.interpretResponse(null);
        // Assuming there's a way to capture console output, check if the "Error 400: No response from server." message was printed
    }

    @Test
    public void testGenerateRequestString_withStationID() {
        String request = getClient.generateRequestString(1, "stationID");
        assertTrue(request.contains("StationID: stationID"));
    }

    @Test
    public void testGenerateRequestString_withoutStationID() {
        String request = getClient.generateRequestString(1, null);
        assertFalse(request.contains("StationID: "));
    }

    @Test
    public void testHandleServerResponse_starts500() {
        assertNull(getClient.handleServerResponse("500 Fake Error"));
    }

    @Test
    public void testHandleServerResponse_noJsonObject() {
        // Assuming the JSONHandler will return a string that's not a valid JSON object
        assertNull(getClient.handleServerResponse("Some response"));
    }

    @Test
    public void testHandleServerResponse_validJsonObject() {
        JsonObject fakeJson = new JsonObject();
        fakeJson.addProperty("fake", "data");
        // Assuming the JSONHandler will return a string that can be converted to this fakeJson
        assertEquals(fakeJson, getClient.handleServerResponse("{\"fake\": \"data\"}"));
    }

    @Test
    public void testGetNetworkHandler() {
        assertEquals(networkHandler, getClient.getNetworkHandler());
    }
}
