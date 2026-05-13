package utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JsonImporter {

    public static List<String[]> importerQuestions() {
        List<String[]> questions = new ArrayList<>();
        try {
            // Lire le fichier JSON depuis resources
            InputStream is = JsonImporter.class
                    .getResourceAsStream("/questions.json");
            String contenu = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            JSONObject json = new JSONObject(contenu);
            JSONArray array = json.getJSONArray("questions");

            for (int i = 0; i < array.length(); i++) {
                JSONObject q = array.getJSONObject(i);
                String[] ligne = {
                        q.getString("question"),
                        q.getString("reponseA"),
                        q.getString("reponseB"),
                        q.getString("reponseC"),
                        q.getString("reponseD"),
                        q.getString("bonneReponse")
                };
                questions.add(ligne);
            }

        } catch (Exception e) { e.printStackTrace(); }
        return questions;
    }
}