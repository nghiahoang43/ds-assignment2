import com.google.gson.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class JSONHandlerTest {

    @Test
    public void testReadFile_nullFilePath() {
        assertThrows(IOException.class, () -> JSONHandler.readFile(null));
    }

    @Test
    public void testReadFile_invalidFilePath() {
        assertThrows(IOException.class, () -> JSONHandler.readFile("invalidPath"));
    }

    // This would require you to have a sample file in your resources for testing
    @Test
    public void testReadFile_validFile() throws IOException {
        String result = JSONHandler.readFile("src/weather_test.txt");
        // Assuming the file contains "sample content"
        assertEquals("id:IDS60901\n" + 
                "local_date_time_full:20230715160000\n" + 
                "air_temp:13.3\n" + 
                "cloud:Partly cloudy\n" + 
                "", result);
    }

    @Test
    public void testParseTextToJSON_nullInput() {
        assertThrows(IllegalArgumentException.class, () -> JSONHandler.parseTextToJSON(null));
    }

    @Test
    public void testParseTextToJSON_invalidLineFormat() {
        assertThrows(IllegalArgumentException.class, () -> JSONHandler.parseTextToJSON("invalidLineWithoutColon"));
    }

    @Test
    public void testParseTextToJSON_validInput() {
        JsonObject result = JSONHandler.parseTextToJSON("key1: value1\nkey2: value2");
        assertEquals("value1", result.get("key1").getAsString());
        assertEquals("value2", result.get("key2").getAsString());
    }

    @Test
    public void testParseJSONtoText_nullJsonObject() {
        assertThrows(IllegalArgumentException.class, () -> JSONHandler.parseJSONtoText(null));
    }

    @Test
    public void testParseJSONtoText_validJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key1", "value1");
        obj.addProperty("key2", "value2");
        String result = JSONHandler.parseJSONtoText(obj);
        assertTrue(result.contains("key1: value1"));
        assertTrue(result.contains("key2: value2"));
    }

    @Test
    public void testExtractJSONContent_noJsonObject() {
        assertNull(JSONHandler.extractJSONContent("This is a sample text without a JSON object."));
    }

    @Test
    public void testExtractJSONContent_validJsonObject() {
        String input = "Some text before {\"key\":\"value\"} Some text after";
        String expected = "{\"key\":\"value\"}";
        assertEquals(expected, JSONHandler.extractJSONContent(input));
    }

}
