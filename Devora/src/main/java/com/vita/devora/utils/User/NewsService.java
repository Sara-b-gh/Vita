package com.vita.devora.utils;

import com.vita.devora.Entities.User.NewsArticle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NewsService {

    private static final String API_KEY = "4edcae3eb8a1452291d7ed0e930dbb92";

    // ← URL corrigée avec top-headlines
    private static final String BASE_URL =
            "https://newsapi.org/v2/everything?q=health+medicine&sortBy=publishedAt&pageSize=15&apiKey=";
    public static List<NewsArticle> fetchMedicalNews() throws Exception {
        return fetch(BASE_URL + API_KEY);
    }

    public static List<NewsArticle> searchNews(String query) throws Exception {
        // ← Recherche sans filtre langue pour plus de résultats
        String url = "https://newsapi.org/v2/everything?q="
                + java.net.URLEncoder.encode(query, "UTF-8")
                + "&language=fr&sortBy=publishedAt&pageSize=15&apiKey=" + API_KEY;
        return fetch(url);
    }

    private static List<NewsArticle> fetch(String urlStr) throws Exception {
        List<NewsArticle> articles = new ArrayList<>();

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        System.out.println("NewsAPI Response Code: " + responseCode);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        System.out.println("NewsAPI Response: " + response.toString().substring(0, Math.min(200, response.length())));

        String json = response.toString();
        String[] articleBlocks = json.split("\\{\"source\"");

        for (int i = 1; i < articleBlocks.length; i++) {
            String block = articleBlocks[i];
            String title = extractValue(block, "title");
            String description = extractValue(block, "description");
            String urlArticle = extractValue(block, "url");
            String publishedAt = extractValue(block, "publishedAt");
            String sourceName = extractValue(block, "name");

            if (title != null && !title.equals("null") && !title.equals("[Removed]")) {
                articles.add(new NewsArticle(title, description, urlArticle, publishedAt, sourceName));
            }
        }

        System.out.println("Articles trouvés: " + articles.size());
        return articles;
    }

    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end)
                .replace("\\u0026", "&")
                .replace("\\u2019", "'")
                .replace("\\u00e9", "é")
                .replace("\\u00e8", "è")
                .replace("\\u00ea", "ê")
                .replace("\\u00e0", "à")
                .replace("\\u00e2", "â")
                .replace("\\u00ee", "î")
                .replace("\\u00f4", "ô")
                .replace("\\u00fb", "û")
                .replace("\\u00e7", "ç")
                .replace("\\n", " ");
    }
}