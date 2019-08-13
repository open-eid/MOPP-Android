package ee.ria.DigiDoc.configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper class for parsing values from configuration json.
 */
class ConfigurationParser {

    private final JSONObject configurationJson;

    ConfigurationParser(String configuration) {
        try {
            configurationJson = new JSONObject(configuration);
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parse JSON object from configuration string", e);
        }
    }

    String parseStringValue(String... parameterNames) {
        return (String) parseValue(parameterNames);
    }

    List<String> parseStringValues(String... parameterNames) {
        JSONArray jsonValues = (JSONArray) parseValue(parameterNames);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < jsonValues.length(); i++) {
            try {
                values.add((String) jsonValues.get(i));
            } catch (JSONException e) {
                throw new RuntimeException("Failed to parse parameter number " + i  + " from parsed list of values (" + jsonValues.toString() + ")", e);
            }
        }
        return values;
    }

    Map<String, String> parseStringValuesToMap(String... parameterNames) {
        JSONObject jsonObject = (JSONObject) parseValue(parameterNames);
        Map<String, String> parsedValues = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                parsedValues.put(key, jsonObject.getString(key));
            } catch (JSONException e) {
                throw new IllegalStateException("Failed to parse value by '" + key + "' from json", e);
            }
        }
        return parsedValues;
    }

    int parseIntValue(String... parameterNames) {
        return (int) parseValue(parameterNames);
    }

    private Object parseValue(String... parameterNames) {
        try {
            JSONObject jsonObject = configurationJson;
            for (int i = 0; i < parameterNames.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(parameterNames[i]);
            }
            return jsonObject.get(parameterNames[parameterNames.length - 1]);
        } catch (JSONException e) {
            StringBuilder combinedValue = new StringBuilder();
            for (String parameterName : parameterNames) {
                combinedValue.append(parameterName);
            }
            throw new RuntimeException("Failed to parse parameter '" + combinedValue + "' from configuration json", e);
        }
    }
}
