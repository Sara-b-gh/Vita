package Services;

import jakarta.mail.*;

import jakarta.mail.internet.InternetAddress;

import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    // ==================== CONFIGURATION ====================

    private static final String FROM_EMAIL = "nourtabaistudy@gmail.com";        // ← CHANGE ICI

    private static final String APP_PASSWORD = "etoy kkrd dbsp vhgh";        // ← CHANGE ICI (App Password Gmail)

    private static final String SMTP_HOST = "smtp.gmail.com";

    private static final String SMTP_PORT = "587";
    private static final String TO_EMAIL = "nadatabai74@gmail.com";
    // =======================================================



    private static Session getSession() {
        Properties props = new Properties();

        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // === SOLUTION POUR L'ERREUR SSL ===
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.debug", "false");   // Mets à true si tu veux plus de détails

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    public static void envoyerNotificationRendezVous(String sujet, String contenu) {
        try {
            Message message = new MimeMessage(getSession());
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(TO_EMAIL));

            message.setSubject(sujet);
            message.setText(contenu);

            Transport.send(message);

            System.out.println("✅ Email envoyé avec succès à " + TO_EMAIL);
            System.out.println("Sujet : " + sujet);

        } catch (MessagingException e) {
            System.err.println("❌ Échec envoi email : " + e.getMessage());
            e.printStackTrace();
        }
    }
}