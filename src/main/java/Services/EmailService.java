package services;

import java.util.logging.Logger;

public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    public EmailService() {
        // Constructeur par défaut
    }

    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            LOGGER.warning("Adresse email destinataire invalide");
            return;
        }

        LOGGER.info("=== SIMULATION D'ENVOI D'EMAIL ===");
        LOGGER.info("Destinataire: " + to);
        LOGGER.info("Sujet: " + subject);
        LOGGER.info("Corps: " + body);
        LOGGER.info("=================================");
    }

    public void envoyerNotification(String email, String sujet, String message) {
        sendEmail(email, sujet, message);
    }
}