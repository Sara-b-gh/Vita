package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    /**
     * Convertit une adresse en coordonnées GPS
     */
    public static double[] geocode(String adresse) {
        if (adresse == null || adresse.isBlank()) return null;

        try {
            String encoded = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
            String url = NOMINATIM_URL + "?q=" + encoded + "&format=json&limit=1";

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                request.setHeader("User-Agent", "VITA-Sante-App/1.0");
                String response = client.execute(request, httpResponse ->
                        EntityUtils.toString(httpResponse.getEntity()));

                JsonArray results = JsonParser.parseString(response).getAsJsonArray();
                if (!results.isEmpty()) {
                    JsonObject first = results.get(0).getAsJsonObject();
                    return new double[]{
                            first.get("lat").getAsDouble(),
                            first.get("lon").getAsDouble()
                    };
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Ouvre Google Maps dans le navigateur avec l'adresse
     */
    public static void ouvrirGoogleMaps(String adresse) {
        try {
            String encoded = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
            String url = "https://www.google.com/maps/search/?api=1&query=" + encoded;
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ouvre Google Maps avec des coordonnées GPS
     */
    public static void ouvrirGoogleMaps(double lat, double lon) {
        try {
            String url = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calcule la distance entre deux points (km)
     */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}