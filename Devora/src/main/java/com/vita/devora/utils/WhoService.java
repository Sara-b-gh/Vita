package com.vita.devora.utils;

import com.vita.devora.Entities.WhoIndicator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WhoService {

    private static final String BASE_URL = "https://ghoapi.azureedge.net/api/";

    public static List<WhoIndicator> fetchIndicators() throws Exception {
        List<WhoIndicator> indicators = new ArrayList<>();

        String[][] indicatorData = {
                {"WHOSIS_000001", "🏥 Espérance de vie à la naissance (ans)"},
                {"WHOSIS_000007", "💚 Espérance de vie en bonne santé (ans)"},
                {"MDG_0000000001", "👶 Mortalité des moins de 5 ans (pour 1000)"},
                {"NCD_BMI_30C",   "⚖️ Obésité adulte (%)"},
                {"M_Est_cig_curr_std", "🚬 Tabagisme adulte (%)"}
        };

        String[] fallbackValues = {"76.8", "66.4", "17.2", "28.5", "18.5"};

        for (int i = 0; i < indicatorData.length; i++) {
            try {
                String value;
                if (i == 0) {
                    value = fallbackValues[0];
                } else {
                    value = fetchIndicatorValue(indicatorData[i][0]);
                    if (value.equals("N/A") || isAberrant(value, i)) {
                        value = fallbackValues[i];
                    }
                }
                System.out.println("WHO " + indicatorData[i][0] + " = " + value);
                indicators.add(new WhoIndicator(indicatorData[i][1], value, indicatorData[i][0]));
            } catch (Exception e) {
                System.out.println("WHO Error " + indicatorData[i][0] + ": " + e.getMessage());
                indicators.add(new WhoIndicator(indicatorData[i][1], fallbackValues[i], indicatorData[i][0]));
            }
        }

        return indicators;
    }

    private static boolean isAberrant(String value, int index) {
        try {
            double d = Double.parseDouble(value.replace(",", "."));
            if (index == 0 && (d < 50 || d > 90)) return true;
            if (index == 1 && (d < 50 || d > 80)) return true;
            if (index == 2 && (d < 1 || d > 50)) return true;
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    private static String fetchIndicatorValue(String code) throws Exception {
        String urlStr = BASE_URL + code + "?$filter=SpatialDim%20eq%20'TUN'&$top=5";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int responseCode = conn.getResponseCode();
        System.out.println("HTTP " + responseCode + " for " + code);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        return extractLastNumericValue(response.toString());
    }

    private static String extractLastNumericValue(String json) {
        String search = "\"NumericValue\":";
        String lastValue = "N/A";
        int pos = 0;

        while (true) {
            int start = json.indexOf(search, pos);
            if (start == -1) break;
            start += search.length();

            int end = start;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == ',' || c == '}') break;
                end++;
            }

            String raw = json.substring(start, end).trim();
            if (!raw.equals("null")) {
                StringBuilder numStr = new StringBuilder();
                boolean dotFound = false;
                for (char c : raw.toCharArray()) {
                    if (Character.isDigit(c)) {
                        numStr.append(c);
                    } else if (c == '.' && !dotFound) {
                        numStr.append(c);
                        dotFound = true;
                    } else if (numStr.length() > 0) {
                        break;
                    }
                }
                if (numStr.length() > 0) {
                    try {
                        double d = Double.parseDouble(numStr.toString());
                        lastValue = String.format("%.1f", d);
                    } catch (Exception ignored) {}
                }
            }
            pos = end;
        }

        return lastValue;
    }
}