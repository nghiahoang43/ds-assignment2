import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSONHandler {

    // Read file content and return as string
    public static String readFile(String filePath) throws IOException {
        if (filePath == null) {
            throw new IOException("Error 400: filePath is null.");
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new IOException("Error reading the file: " + e.getMessage(), e);
        }
        return content.toString();
    }

    // Convert text to JSON Object
    public static JSONObject parseTextToJSON(String inputText) throws IllegalArgumentException {
        if (inputText == null) {
            throw new IllegalArgumentException("Input text is null.");
        }

        Map<String, Object> jsonData = new HashMap<>();
        for (String line : inputText.split("\n")) {
            String[] parts = line.split(":", 2);

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid line format: " + line);
            }

            jsonData.put(parts[0].trim(), parts[1].trim());
        }

        return new JSONObject(jsonData);
    }

    // Convert JSON Object to text
    public static String parseJSONtoText(JSONObject jsonObject) throws IllegalArgumentException {
        if (jsonObject == null) {
            throw new IllegalArgumentException("Error 400: jsonObject is null.");
        }

        StringBuilder output = new StringBuilder();
        Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            String valueStr = value.toString();
            output.append(key).append(": ").append(valueStr).append("\n");
        }

        return output.toString();
    }

    // Extract JSON content from a string
    public static String extractJSONContent(String data) {
        int startIdx = data.indexOf("{");
        int endIdx = data.lastIndexOf("}");

        if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
            return data.substring(startIdx, endIdx + 1);
        }

        return null; // No valid JSON content found
    }

    // Parse JSON object from string data
    public static JSONObject parseJSONObject(String jsonData) {
        return (jsonData != null && !jsonData.isEmpty()) ? new JSONObject(jsonData) : null;
    }
}
