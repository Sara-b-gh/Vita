package services;

import entities.RendezVous;
import entities.User;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SmsRappelService {

    private static final String ACCOUNT_SID = "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"; // à remplacer
    private static final String AUTH_TOKEN  = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";   // à remplacer
    private static final String FROM_NUMBER = "+1XXXXXXXXXX";                       // numéro Twilio

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    /**
     * Planifie deux rappels SMS :
     *  - 1 jour avant le rendez-vous
     *  - 1 heure avant le rendez-vous
     */
    public static void planifierRappels(RendezVous rv, User patient) {
        if (patient == null || patient.getTelephone() == null || patient.getTelephone().isBlank()) {
            System.out.println("[SMS] Aucun numéro pour le patient #" + rv.getPatient_id());
            return;
        }

        LocalDateTime dateRdv = rv.getDate_rdv();
        if (dateRdv == null) return;

        String nomPatient   = patient.getPrenom() + " " + patient.getNom();
        String dateFormatee = dateRdv.format(FMT);
        String motif        = rv.getMotif() != null ? rv.getMotif() : "consultation";
        String telephone    = patient.getTelephone();

        // ── Rappel J-1 ────────────────────────────────────────────────
        String msg1Jour = String.format(
                "Bonjour %s, rappel : vous avez un rendez-vous médical demain %s (motif : %s). " +
                        "Pour annuler, contactez votre médecin.",
                nomPatient, dateFormatee, motif
        );
        planifierSms(telephone, msg1Jour, dateRdv.minusDays(1), "J-1 RDV #" + rv.getId_rdv());

        // ── Rappel H-1 ────────────────────────────────────────────────
        String msg1Heure = String.format(
                "Bonjour %s, votre rendez-vous médical est dans 1 heure (%s). " +
                        "Pensez à vous préparer. Motif : %s.",
                nomPatient, dateFormatee, motif
        );
        planifierSms(telephone, msg1Heure, dateRdv.minusHours(1), "H-1 RDV #" + rv.getId_rdv());
    }

    /**
     * Planifie l'envoi d'un SMS à une date/heure précise.
     * Si le moment est déjà passé, l'envoi est ignoré.
     */
    private static void planifierSms(String to, String message, LocalDateTime quand, String label) {
        LocalDateTime maintenant = LocalDateTime.now();
        if (!quand.isAfter(maintenant)) {
            System.out.println("[SMS] Rappel " + label + " déjà passé, ignoré.");
            return;
        }

        long delaiMs = java.time.Duration.between(maintenant, quand).toMillis();
        System.out.printf("[SMS] Rappel « %s » planifié dans %d min.%n",
                label, TimeUnit.MILLISECONDS.toMinutes(delaiMs));

        Timer timer = new Timer(true); // daemon thread
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean ok = envoyerSms(to, message);
                System.out.printf("[SMS] Envoi %s → %s : %s%n",
                        label, to, ok ? "✓ OK" : "✗ ÉCHEC");
            }
        }, delaiMs);
    }

    /**
     * Appel HTTP vers l'API Twilio.
     * @return true si HTTP 201 (créé), false sinon
     */
    private static boolean envoyerSms(String to, String message) {
        try {
            String body = "To="    + URLEncoder.encode(to,          StandardCharsets.UTF_8) +
                    "&From=" + URLEncoder.encode(FROM_NUMBER,  StandardCharsets.UTF_8) +
                    "&Body=" + URLEncoder.encode(message,      StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", buildBasicAuth(ACCOUNT_SID, AUTH_TOKEN))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 201;

        } catch (Exception e) {
            System.err.println("[SMS] Erreur envoi : " + e.getMessage());
            return false;
        }
    }

    private static String buildBasicAuth(String user, String pass) {
        String credentials = user + ":" + pass;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}