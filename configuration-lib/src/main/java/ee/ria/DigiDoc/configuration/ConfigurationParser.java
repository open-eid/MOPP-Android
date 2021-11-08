package ee.ria.DigiDoc.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class for parsing values from configuration json.
 */
public class ConfigurationParser {

    private final JsonObject configurationJson;

    public ConfigurationParser(String configuration) {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new StringReader(configuration));
        reader.setLenient(true);
        configurationJson = gson.fromJson(reader, JsonObject.class);
    }

    String parseStringValue(String... parameterNames) {
        return (String) parseValue(parameterNames);
    }

    List<String> parseStringValues(String... parameterNames) {
        JsonArray jsonValues = (JsonArray) parseValue(parameterNames);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < jsonValues.size(); i++) {
            values.add(jsonValues.get(i).getAsString());
        }
        return values;
    }

    Map<String, String> parseStringValuesToMap(String... parameterNames) {
        return (Map<String, String>) parseValue(parameterNames);
    }

    public int parseIntValue(String... parameterNames) {
        return Integer.parseInt((String) parseValue(parameterNames));
    }

    private Object parseValue(String... parameterNames) {
        JsonObject jsonObject = configurationJson;
        for (int i = 0; i < parameterNames.length - 1; i++) {
            jsonObject = jsonObject.getAsJsonObject(parameterNames[i]);
        }
        JsonElement element = jsonObject.get(parameterNames[parameterNames.length - 1]);

        if (element == null) {
            throw new RuntimeException("Failed to parse parameter 'MISSING-VALUE' from configuration json");
        }

        if (element instanceof JsonArray) {
            return element.getAsJsonArray();
        } else if (element instanceof JsonObject) {
            return new Gson().fromJson(element, Map.class);
        }
        return element.getAsString();
    }
}
