package com.vita.devora.Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TranslationService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";

    public String translate(String text, String sourceLang, String targetLang) {
        try {
            if ("auto".equals(sourceLang)) {
                sourceLang = detectLang(text);
            }
            if (sourceLang.equals(targetLang)) return text;

            String langPair = sourceLang + "|" + targetLang;
            String urlStr = API_URL
                    + "?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&langpair=" + URLEncoder.encode(langPair, StandardCharsets.UTF_8);

            System.out.println("DEBUG URL: " + urlStr); // pour vérifier dans la console

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int code = conn.getResponseCode();
            System.out.println("DEBUG HTTP code: " + code);
            if (code != 200) return "[Erreur HTTP " + code + "]";

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            String json = sb.toString();
            System.out.println("DEBUG JSON: " + json); // voir la réponse brute

            return parseTranslatedText(json, text);

        } catch (Exception e) {
            System.err.println("Erreur traduction: " + e.getMessage());
            e.printStackTrace();
            return "[Erreur: " + e.getMessage() + "]";
        }
    }

    private String parseTranslatedText(String json, String fallback) {
        try {
            // Cherche "translatedText":"VALEUR"
            String key = "\"translatedText\":\"";
            int start = json.indexOf(key);
            if (start == -1) {
                System.err.println("DEBUG: clé translatedText non trouvée dans: " + json);
                return fallback;
            }

            start += key.length();

            // Trouver la fin de la valeur en ignorant les \" échappés
            StringBuilder result = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == '"') { result.append('"'); i++; continue; }
                    if (next == 'n')  { result.append('\n'); i++; continue; }
                    if (next == '\\') { result.append('\\'); i++; continue; }
                }
                if (c == '"') break; // fin de la valeur JSON
                result.append(c);
            }

            String translated = result.toString()
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'");

            System.out.println("DEBUG traduit: " + translated);
            return translated.isBlank() ? fallback : translated;

        } catch (Exception e) {
            System.err.println("Erreur parsing: " + e.getMessage());
            return fallback;
        }
    }

    private String detectLang(String text) {
        if (text == null || text.isBlank()) return "en";
        int arabic = 0, french = 0;
        for (char c : text.toCharArray()) {
            if (c >= 0x0600 && c <= 0x06FF) arabic++;
            if ("éèêëàâùûüîïôçœæ".indexOf(Character.toLowerCase(c)) >= 0) french++;
        }
        if (arabic > 2) return "ar";
        if (french > 1) return "fr";
        return "en";
    }

    public String toEnglish(String text) { return translate(text, "auto", "en"); }
    public String toFrench(String text)  { return translate(text, "auto", "fr"); }
    public String toArabic(String text)  { return translate(text, "auto", "ar"); }
}
