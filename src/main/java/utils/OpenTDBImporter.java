package utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenTDBImporter {

    // Santé = Science & Nature (17) + Biology inclus
    private static final String[] URLS = {
            "https://opentdb.com/api.php?amount=10&category=17&type=multiple",
            "https://opentdb.com/api.php?amount=10&category=27&type=multiple"
    };

    // Mots clés santé/moral en anglais
    private static final String[] MOTS_SANTE = {
            "health", "body", "brain", "heart", "blood", "muscle", "bone",
            "disease", "virus", "bacteria", "medicine", "doctor", "hospital",
            "organ", "lung", "liver", "kidney", "skin", "nerve", "cell",
            "vitamin", "protein", "calorie", "oxygen", "hormone", "sleep",
            "stress", "mental", "anxiety", "depression", "exercise", "diet",
            "human", "anatomy", "biology", "medical", "surgery", "vaccine",
            "immune", "cancer", "diabetes", "therapy", "symptom", "treatment",
            "artery", "vein", "neuron", "enzyme", "stomach", "intestine"
    };

    public static List<String[]> importerQuestions() {
        List<String[]> toutesLesQuestions = new ArrayList<>();

        for (String url : URLS) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body().string();

                JSONObject json = new JSONObject(body);
                int code = json.getInt("response_code");
                if (code != 0) continue;

                JSONArray results = json.getJSONArray("results");

                for (int i = 0; i < results.length(); i++) {
                    JSONObject q = results.getJSONObject(i);

                    String question     = decodeHtml(q.getString("question"));
                    String bonneReponse = decodeHtml(q.getString("correct_answer"));
                    JSONArray incorrectes = q.getJSONArray("incorrect_answers");

                    if (incorrectes.length() < 3) continue;

                    // ✅ Filtrer : garder seulement questions santé/moral
                    if (!estLieeASante(question)) continue;

                    List<String> reponses = new ArrayList<>();
                    reponses.add(bonneReponse);
                    for (int j = 0; j < incorrectes.length(); j++) {
                        reponses.add(decodeHtml(incorrectes.getString(j)));
                    }

                    Collections.shuffle(reponses);

                    int indexBonne = reponses.indexOf(bonneReponse);
                    String[] lettres = {"A", "B", "C", "D"};
                    String lettreBonne = lettres[indexBonne];

                    String[] ligne = {
                            question,
                            reponses.get(0),
                            reponses.get(1),
                            reponses.get(2),
                            reponses.get(3),
                            lettreBonne
                    };
                    toutesLesQuestions.add(ligne);
                }

                Thread.sleep(1000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return toutesLesQuestions;
    }

    // ✅ Vérifie si la question est liée à la santé/moral
    private static boolean estLieeASante(String question) {
        String qLower = question.toLowerCase();
        for (String mot : MOTS_SANTE) {
            if (qLower.contains(mot)) return true;
        }
        return false;
    }

    private static String decodeHtml(String text) {
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&eacute;", "é")
                .replace("&egrave;", "è")
                .replace("&agrave;", "à")
                .replace("&ccedil;", "ç")
                .replace("&ocirc;", "ô")
                .replace("&ucirc;", "û")
                .replace("&ecirc;", "ê")
                .replace("&icirc;", "î")
                .replace("&acirc;", "â")
                .replace("&ugrave;", "ù")
                .replace("&rsquo;", "'")
                .replace("&laquo;", "«")
                .replace("&raquo;", "»")
                .replace("&hellip;", "...");
    }
}
