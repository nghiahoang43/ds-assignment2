import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.*;

public class JSONHandler {
    private static final Gson gson = new Gson();

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

    public static JsonObject parseTextToJSON(String inputText) throws IllegalArgumentException {
        if (inputText == null) {
            throw new IllegalArgumentException("Input text is null.");
        }

        Map<String, Object> jsonData = new LinkedHashMap<>();
        for (String line : inputText.split("\n")) {
            String[] parts = line.split(":", 2);

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid line format: " + line);
            }

            jsonData.put(parts[0].trim(), parts[1].trim());
        }

        return gson.toJsonTree(jsonData).getAsJsonObject();
    }

    public static String parseJSONtoText(JsonObject jsonObject) throws IllegalArgumentException {
        if (jsonObject == null) {
            throw new IllegalArgumentException("Error 400: jsonObject is null.");
        }

        StringBuilder output = new StringBuilder();
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();

        for (Map.Entry<String, JsonElement> entry : entries) {
            String key = entry.getKey();
            JsonElement valueElement = entry.getValue();

            String valueStr;
            if (valueElement.isJsonPrimitive()) {
                valueStr = valueElement.getAsJsonPrimitive().getAsString();
            } else {
                valueStr = valueElement.toString();
            }

            output.append(key).append(": ").append(valueStr).append("\n");
        }

        return output.toString();
    }

    public static String extractJSONContent(String data) {
        int startIdx = data.indexOf("{");
        int endIdx = data.lastIndexOf("}");

        if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
            return data.substring(startIdx, endIdx + 1);
        }

        return null;
    }

}
