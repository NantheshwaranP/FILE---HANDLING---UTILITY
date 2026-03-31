import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * WeatherApp - Fetches current weather data from Open-Meteo API
 * No API key required. Uses built-in Java HTTP and manual JSON parsing.
 */
public class WeatherApp {

    // Open-Meteo free weather API
    private static final String API_URL =
        "https://api.open-meteo.com/v1/forecast" +
        "?latitude=40.7128&longitude=-74.0060" +  // New York City
        "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,apparent_temperature" +
        "&temperature_unit=celsius&wind_speed_unit=kmh&timezone=America%2FNew_York";

    public static void main(String[] args) {
        System.out.println("Fetching weather data for New York City...\n");

        try {
            String jsonResponse = fetchData(API_URL);
            Map<String, String> weatherData = parseWeather(jsonResponse);
            displayWeather(weatherData);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Makes an HTTP GET request and returns the response body as a String.
     */
    private static String fetchData(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("HTTP request failed with status: " + status);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            conn.disconnect();
        }

        return response.toString();
    }

    /**
     * Parses relevant fields from the JSON response using simple string extraction.
     * No external libraries needed.
     */
    private static Map<String, String> parseWeather(String json) {
        Map<String, String> data = new HashMap<>();

        data.put("latitude",    extractValue(json, "latitude"));
        data.put("longitude",   extractValue(json, "longitude"));
        data.put("timezone",    extractStringValue(json, "timezone"));
        data.put("temperature", extractNestedValue(json, "current", "temperature_2m"));
        data.put("feels_like",  extractNestedValue(json, "current", "apparent_temperature"));
        data.put("humidity",    extractNestedValue(json, "current", "relative_humidity_2m"));
        data.put("wind_speed",  extractNestedValue(json, "current", "wind_speed_10m"));
        data.put("weather_code",extractNestedValue(json, "current", "weather_code"));
        data.put("time",        extractNestedStringValue(json, "current", "time"));

        return data;
    }

    /**
     * Extracts a numeric value for a top-level key: "key":value
     */
    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return "N/A";
        int start = idx + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return end == -1 ? "N/A" : json.substring(start, end).trim();
    }

    /**
     * Extracts a string value for a top-level key: "key":"value"
     */
    private static String extractStringValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "N/A";
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "N/A" : json.substring(start, end);
    }

    /**
     * Extracts a numeric value from within a nested object block.
     */
    private static String extractNestedValue(String json, String block, String key) {
        int blockIdx = json.indexOf("\"" + block + "\":");
        if (blockIdx == -1) return "N/A";
        String sub = json.substring(blockIdx);
        return extractValue(sub, key);
    }

    /**
     * Extracts a string value from within a nested object block.
     */
    private static String extractNestedStringValue(String json, String block, String key) {
        int blockIdx = json.indexOf("\"" + block + "\":");
        if (blockIdx == -1) return "N/A";
        String sub = json.substring(blockIdx);
        return extractStringValue(sub, key);
    }

    /**
     * Maps WMO weather codes to human-readable descriptions.
     * https://open-meteo.com/en/docs#weathervariables
     */
    private static String describeWeatherCode(String code) {
        if (code == null || code.equals("N/A")) return "Unknown";
        return switch (code.trim()) {
            case "0"       -> "Clear sky";
            case "1"       -> "Mainly clear";
            case "2"       -> "Partly cloudy";
            case "3"       -> "Overcast";
            case "45","48" -> "Foggy";
            case "51","53","55" -> "Drizzle";
            case "61","63","65" -> "Rain";
            case "71","73","75" -> "Snow";
            case "80","81","82" -> "Rain showers";
            case "95"      -> "Thunderstorm";
            case "96","99" -> "Thunderstorm with hail";
            default        -> "Code " + code;
        };
    }

    /**
     * Prints the weather data in a structured, readable format.
     */
    private static void displayWeather(Map<String, String> data) {
        String separator = "+--------------------------+----------------------+";
        String condition = describeWeatherCode(data.get("weather_code"));

        System.out.println(separator);
        System.out.printf("| %-24s | %-20s |%n", "Field", "Value");
        System.out.println(separator);
        System.out.printf("| %-24s | %-20s |%n", "Location (lat, lon)",
            data.get("latitude") + ", " + data.get("longitude"));
        System.out.printf("| %-24s | %-20s |%n", "Timezone",       data.get("timezone"));
        System.out.printf("| %-24s | %-20s |%n", "Time",           data.get("time"));
        System.out.printf("| %-24s | %-20s |%n", "Condition",      condition);
        System.out.printf("| %-24s | %-20s |%n", "Temperature (°C)",data.get("temperature"));
        System.out.printf("| %-24s | %-20s |%n", "Feels Like (°C)", data.get("feels_like"));
        System.out.printf("| %-24s | %-20s |%n", "Humidity (%)",   data.get("humidity"));
        System.out.printf("| %-24s | %-20s |%n", "Wind Speed (km/h)",data.get("wind_speed"));
        System.out.println(separator);
    }
}
