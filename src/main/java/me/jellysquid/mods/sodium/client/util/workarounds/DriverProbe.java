package me.jellysquid.mods.sodium.client.util.workarounds;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DriverProbe {
    static String encodeResponse(Map<String, String> fields) {
        var response = new StringBuilder();

        for (var entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String encodedValue;

            if (value == null) {
                encodedValue = "null";
            } else {
                encodedValue = Base64.getEncoder()
                        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
            }

            response.append(key).append(':').append(encodedValue).append(';');
        }

        return response.toString();
    }

    static Map<String, String> decodeResponse(String response) {
        var fields = new HashMap<String, String>();
        var lines = response.split(";");

        for (var line : lines) {
            var parts = line.split(":");
            var key = parts[0];
            var encodedValue = parts[1];

            String value;

            if (encodedValue.equals("null")) {
                value = null;
            } else {
                value = new String(Base64.getDecoder()
                        .decode(encodedValue), StandardCharsets.UTF_8);
            }

            fields.put(key, value);
        }

        return fields;
    }
}
