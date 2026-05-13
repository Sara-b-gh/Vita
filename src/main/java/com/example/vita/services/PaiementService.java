package com.example.vita.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Service de paiement VITA.
 *
 * Mode de fonctionnement :
 *  - Si la clé Konnect est configurée dans config.properties → appel API réel.
 *  - Sinon → simulation locale (utile en développement sans connexion).
 *
 * Konnect est une passerelle de paiement tunisienne qui supporte :
 *  carte bancaire, wallet, e-DINAR, Sobflous.
 *
 * Flux réel :
 *  1. initierPaiement()  → Konnect retourne un payUrl
 *  2. Ouvrir payUrl dans WebView JavaFX (voir CommandeController)
 *  3. L'utilisateur paye sur la page Konnect
 *  4. Konnect redirige vers successUrl ou failUrl
 *  5. verifierStatut()   → confirme le paiement côté serveur
 */
public class PaiementService {

    // ─────────────────────────────────────────────────────────────
    //  Types de retour
    // ─────────────────────────────────────────────────────────────

    public enum StatutPaiement { SUCCES, ECHEC, EN_ATTENTE }

    public static class ResultatPaiement {
        public final StatutPaiement statut;
        public final String         reference;
        public final String         message;
        public final double         montant;
        public final String         payUrl;     // URL page Konnect (paiement réel)

        public ResultatPaiement(StatutPaiement statut, String reference,
                                String message, double montant, String payUrl) {
            this.statut    = statut;
            this.reference = reference;
            this.message   = message;
            this.montant   = montant;
            this.payUrl    = payUrl;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Client HTTP
    // ─────────────────────────────────────────────────────────────

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON_TYPE =
            MediaType.get("application/json; charset=utf-8");

    // ─────────────────────────────────────────────────────────────
    //  Point d'entrée principal
    // ─────────────────────────────────────────────────────────────

    /**
     * Initie un paiement.
     *
     * @param montant        Montant en DT (ex: 25.50)
     * @param modePaiement   "Carte bancaire", "Especes", "Virement", "Cheque"
     * @param description    Description de la commande
     * @return ResultatPaiement avec payUrl si Konnect, ou simulation locale
     */
    public static ResultatPaiement initierPaiement(double montant,
                                                   String modePaiement,
                                                   String description) {
        // Validation de base
        if (montant <= 0) {
            return new ResultatPaiement(StatutPaiement.ECHEC, null,
                    "Montant invalide (doit être > 0).", montant, null);
        }

        // Paiements non-carte → simulation locale (espèces, virement, chèque)
        if (!"Carte bancaire".equals(modePaiement)) {
            return simulerPaiementLocal(montant, modePaiement);
        }

        // Carte bancaire : utiliser Konnect si configuré
        if (Configservice.isConfigured("konnect.api.key")) {
            return appelerKonnect(montant, description);
        }

        // Fallback : simulation locale
        System.out.println("[Paiement] Clé Konnect non configurée → simulation locale.");
        return simulerPaiementLocal(montant, modePaiement);
    }

    // ─────────────────────────────────────────────────────────────
    //  Appel API Konnect (paiement réel Tunisie)
    // ─────────────────────────────────────────────────────────────

    private static ResultatPaiement appelerKonnect(double montant, String description) {
        String apiKey   = Configservice.get("konnect.api.key");
        String walletId = Configservice.get("konnect.wallet.id");
        String baseUrl  = Configservice.get("konnect.base.url");

        // Konnect travaille en millimes (1 DT = 1000 millimes)
        int montantMillimes = (int) Math.round(montant * 1000);

        // Corps de la requête JSON
        JsonObject body = new JsonObject();
        body.addProperty("receiverWalletId",  walletId);
        body.addProperty("token",             "TND");
        body.addProperty("amount",            montantMillimes);
        body.addProperty("type",              "immediate");
        body.addProperty("description",       description != null ? description : "Commande VITA");
        body.addProperty("lifespan",          10);   // minutes
        body.addProperty("checkoutForm",      true);
        body.addProperty("successUrl",        "http://localhost/vita/success");
        body.addProperty("failUrl",           "http://localhost/vita/fail");

        // Méthodes acceptées selon sandbox ou production
        com.google.gson.JsonArray methods = new com.google.gson.JsonArray();
        methods.add("bank_card");
        methods.add("wallet");
        methods.add("e-DINAR");
        body.add("acceptedPaymentMethods", methods);

        RequestBody requestBody = RequestBody.create(body.toString(), JSON_TYPE);

        Request request = new Request.Builder()
                .url(baseUrl + "/payments/init-payment")
                .addHeader("x-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";

            if (!response.isSuccessful()) {
                System.err.println("[Konnect] Erreur HTTP " + response.code() + " : " + responseBody);
                return new ResultatPaiement(StatutPaiement.ECHEC, null,
                        "Erreur Konnect (" + response.code() + "). Vérifiez votre clé API.", montant, null);
            }

            JsonObject json      = JsonParser.parseString(responseBody).getAsJsonObject();
            String     paymentId = json.has("paymentRef") ? json.get("paymentRef").getAsString() : null;
            String     payUrl    = json.has("payUrl")     ? json.get("payUrl").getAsString()     : null;

            if (payUrl == null) {
                return new ResultatPaiement(StatutPaiement.ECHEC, null,
                        "Réponse Konnect invalide : payUrl absent.", montant, null);
            }

            return new ResultatPaiement(
                    StatutPaiement.EN_ATTENTE,
                    paymentId,
                    "Redirection vers la page de paiement Konnect...",
                    montant,
                    payUrl
            );

        } catch (IOException e) {
            System.err.println("[Konnect] Erreur réseau : " + e.getMessage());
            return new ResultatPaiement(StatutPaiement.ECHEC, null,
                    "Erreur réseau : " + e.getMessage(), montant, null);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Simulation locale (développement / modes non-carte)
    // ─────────────────────────────────────────────────────────────

    public static ResultatPaiement simulerPaiementLocal(double montant, String modePaiement) {
        if (montant <= 0) {
            return new ResultatPaiement(StatutPaiement.ECHEC, null,
                    "Montant invalide.", montant, null);
        }
        String ref = genererReference();
        return new ResultatPaiement(
                StatutPaiement.SUCCES,
                ref,
                "Paiement de " + String.format("%.2f", montant)
                        + " DT effectué (" + modePaiement + "). Ref : " + ref,
                montant,
                null
        );
    }

    // Conservé pour compatibilité avec CommandeController existant
    public static ResultatPaiement simulerPaiement(double montant,
                                                   String modePaiement,
                                                   String numeroCarte) {
        return simulerPaiementLocal(montant, modePaiement);
    }

    // ─────────────────────────────────────────────────────────────
    //  Utilitaires
    // ─────────────────────────────────────────────────────────────

    private static String genererReference() {
        String date   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int    random = new Random().nextInt(9000) + 1000;
        return "VITA-" + date + "-" + random;
    }
}