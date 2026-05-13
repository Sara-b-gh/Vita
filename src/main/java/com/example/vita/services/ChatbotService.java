package com.example.vita.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service chatbot VITA.
 *
 * Fonctionnement :
 *  - Si la clé OpenAI est configurée → appel GPT-4o-mini (IA réelle).
 *  - Sinon → réponses locales par mots-clés (mode hors-ligne).
 *
 * L'historique de conversation est conservé en mémoire pour permettre
 * un dialogue multi-tours (le chatbot se souvient du contexte).
 *
 * Usage depuis le controller :
 *  // Dans un Task<String> JavaFX (thread séparé) :
 *  String reponse = ChatbotService.repondre(messageUtilisateur);
 *
 *  // Pour réinitialiser la conversation :
 *  ChatbotService.reinitialiserConversation();
 */
public class ChatbotService {

    // ─────────────────────────────────────────────────────────────
    //  Historique de conversation (multi-tour)
    // ─────────────────────────────────────────────────────────────

    private static final List<JsonObject> historique = new ArrayList<>();

    // Prompt système qui définit le comportement du chatbot
    private static final String SYSTEM_PROMPT =
            "Tu es VITA Assistant, un assistant médical intelligent intégré dans le logiciel " +
                    "de gestion hospitalière VITA. Tu es spécialisé dans la gestion des médicaments " +
                    "et équipements médicaux en Tunisie.\n\n" +
                    "Tes capacités :\n" +
                    "- Répondre aux questions sur les médicaments (dosage, forme, statut, stock)\n" +
                    "- Aider avec la gestion des équipements médicaux\n" +
                    "- Guider l'utilisateur dans l'interface VITA\n" +
                    "- Expliquer les procédures de commande et paiement\n" +
                    "- Donner des informations médicales générales (pas de diagnostic)\n\n" +
                    "Règles importantes :\n" +
                    "- Réponds toujours en français\n" +
                    "- Sois concis (max 3-4 phrases)\n" +
                    "- Ne pose pas de diagnostic médical\n" +
                    "- Si tu ne sais pas, dis-le clairement\n" +
                    "- Pour les urgences, oriente vers un médecin";

    // ─────────────────────────────────────────────────────────────
    //  Client HTTP
    // ─────────────────────────────────────────────────────────────

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON_TYPE =
            MediaType.get("application/json; charset=utf-8");

    // ─────────────────────────────────────────────────────────────
    //  Méthode principale
    // ─────────────────────────────────────────────────────────────

    /**
     * Répond au message de l'utilisateur.
     * Appel bloquant → à exécuter dans un thread séparé (Task JavaFX).
     */
    public static String repondre(String messageUtilisateur) {
        if (messageUtilisateur == null || messageUtilisateur.isBlank()) {
            return "Veuillez poser une question.";
        }

        // Choisir le mode selon la configuration
        if (Configservice.isConfigured("openai.api.key")) {
            return repondreAvecOpenAI(messageUtilisateur);
        } else {
            System.out.println("[Chatbot] Clé OpenAI non configurée → mode hors-ligne.");
            return repondreLocalement(messageUtilisateur);
        }
    }

    /**
     * Efface l'historique de conversation (nouvelle session).
     */
    public static void reinitialiserConversation() {
        historique.clear();
    }

    // ─────────────────────────────────────────────────────────────
    //  Mode IA - OpenAI GPT-4o-mini
    // ─────────────────────────────────────────────────────────────

    private static String repondreAvecOpenAI(String messageUtilisateur) {
        String apiKey = Configservice.get("openai.api.key");
        String model  = Configservice.get("openai.model");
        if (model.isBlank()) model = "gpt-4o-mini";

        int maxTokens;
        try {
            maxTokens = Integer.parseInt(Configservice.get("openai.max.tokens"));
        } catch (NumberFormatException e) {
            maxTokens = 600;
        }

        // Ajouter le message utilisateur à l'historique
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", messageUtilisateur);
        historique.add(userMsg);

        // Construire le tableau messages (system + historique)
        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        // Limiter l'historique aux 20 derniers échanges (évite dépassement token)
        int debut = Math.max(0, historique.size() - 20);
        for (int i = debut; i < historique.size(); i++) {
            messages.add(historique.get(i));
        }

        // Corps de la requête
        JsonObject body = new JsonObject();
        body.addProperty("model",      model);
        body.addProperty("max_tokens", maxTokens);
        body.add("messages", messages);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";

            if (!response.isSuccessful()) {
                System.err.println("[OpenAI] Erreur " + response.code() + " : " + responseBody);
                historique.remove(historique.size() - 1); // annuler l'ajout
                return "Désolé, le service IA est temporairement indisponible. (Erreur " + response.code() + ")";
            }

            JsonObject json    = JsonParser.parseString(responseBody).getAsJsonObject();
            String     reponse = json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString().trim();

            // Ajouter la réponse à l'historique
            JsonObject assistantMsg = new JsonObject();
            assistantMsg.addProperty("role", "assistant");
            assistantMsg.addProperty("content", reponse);
            historique.add(assistantMsg);

            return reponse;

        } catch (IOException e) {
            System.err.println("[OpenAI] Erreur réseau : " + e.getMessage());
            historique.remove(historique.size() - 1);
            return "Erreur réseau : impossible de joindre le service IA. Vérifiez votre connexion.";
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Mode hors-ligne - réponses par mots-clés
    // ─────────────────────────────────────────────────────────────

    private static String repondreLocalement(String message) {
        String lower = message.toLowerCase().trim();

        if (lower.contains("bonjour") || lower.contains("hello") || lower.contains("salut"))
            return "Bonjour ! Je suis VITA Assistant. Comment puis-je vous aider avec la gestion de vos médicaments ou équipements ?";
        if (lower.contains("medicament") || lower.contains("médicament"))
            return "Gérez vos médicaments depuis la section 'Médicaments' du menu. Vous pouvez ajouter, modifier, supprimer et basculer entre vue tableau et vue cards.";
        if (lower.contains("stock") || lower.contains("epuise") || lower.contains("épuisé"))
            return "Vérifiez le stock dans la liste des médicaments. Les articles épuisés sont marqués 'epuise' en rouge. Modifiez la quantité depuis le formulaire.";
        if (lower.contains("equipement") || lower.contains("équipement"))
            return "Les équipements sont gérés depuis la section 'Equipements'. Chaque équipement a un état : disponible, en_maintenance ou hors_service.";
        if (lower.contains("commande") || lower.contains("commander"))
            return "Pour passer une commande : allez dans 'Commandes', choisissez un article, ajoutez-le, puis cliquez 'Payer'. Plusieurs modes de paiement sont disponibles.";
        if (lower.contains("paiement") || lower.contains("payer") || lower.contains("carte"))
            return "Le paiement par carte utilise Konnect (Tunisie). Configurez votre clé API dans config.properties pour activer les paiements réels.";
        if (lower.contains("image") || lower.contains("photo"))
            return "Les images des médicaments et équipements sont chargées depuis Unsplash. Ajoutez votre clé API Unsplash dans config.properties.";
        if (lower.contains("admin") || lower.contains("mot de passe") || lower.contains("connexion"))
            return "Accédez à l'espace admin depuis le menu principal. Le mot de passe par défaut est dans les paramètres de l'application.";
        if (lower.contains("ajouter") || lower.contains("créer") || lower.contains("nouveau"))
            return "Pour ajouter un élément : remplissez le formulaire à droite et cliquez sur '+ Ajouter'. Tous les champs obligatoires sont marqués.";
        if (lower.contains("modifier") || lower.contains("éditer"))
            return "Cliquez sur un élément dans la liste ou la card pour le sélectionner, modifiez les champs dans le formulaire, puis cliquez 'Modifier'.";
        if (lower.contains("supprimer") || lower.contains("effacer"))
            return "Sélectionnez l'élément à supprimer puis cliquez sur le bouton 'Supprimer'. Une confirmation sera demandée.";
        if (lower.contains("merci"))
            return "De rien ! N'hésitez pas si vous avez d'autres questions.";
        if (lower.contains("au revoir") || lower.contains("bye"))
            return "Au revoir ! Bonne journée.";
        if (lower.contains("aide") || lower.contains("help"))
            return "Je peux vous aider avec : médicaments, équipements, commandes, paiements, images. Configurez une clé OpenAI dans config.properties pour des réponses plus intelligentes !";

        return "Je n'ai pas compris votre question. Essayez : 'médicament', 'équipement', 'commande', 'paiement', ou configurez OpenAI pour des réponses IA complètes.";
    }
}