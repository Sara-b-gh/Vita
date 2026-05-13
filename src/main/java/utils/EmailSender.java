package utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailSender {

    private static final String FROM_EMAIL = "selmenlimayem09@gmail.com"; // ← ton Gmail
    private static final String APP_PASSWORD = "oloj gadk dabb goic"; // ← mot de passe application

    public static void envoyerScore(String toEmail, String username,
                                    int score, int total, String details) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            message.setSubject("🏥 Vita Quiz - Vos résultats");

            double pct = total > 0 ? (double) score / total * 100 : 0;
            String mention;
            if (pct == 100) mention = "🌟 Parfait !";
            else if (pct >= 70) mention = "👍 Très bien !";
            else if (pct >= 50) mention = "😊 Pas mal !";
            else mention = "💪 Continuez vos efforts !";

            String contenu =
                    "<!DOCTYPE html><html><body style='font-family:Arial;background:#f0ebf4;padding:20px;'>" +
                            "<div style='max-width:600px;margin:auto;background:white;border-radius:12px;" +
                            "border-top:6px solid #AD1457;padding:30px;'>" +
                            "<h2 style='color:#6D1B3A;'>🏥 Vita Quiz - Résultats</h2>" +
                            "<p>Bonjour <b>" + username + "</b>,</p>" +
                            "<p>Voici vos résultats du quiz médical :</p>" +
                            "<div style='background:#f8f5f7;border-radius:8px;padding:20px;text-align:center;'>" +
                            "<h1 style='color:#AD1457;font-size:36px;margin:0;'>" +
                            score + " / " + total + "</h1>" +
                            "<p style='font-size:20px;color:#6D1B3A;'>" + (int)pct + "%</p>" +
                            "<p style='font-size:18px;'>" + mention + "</p>" +
                            "</div>" +
                            "<hr style='border-color:#f0f0f0;margin:20px 0;'/>" +
                            "<h3 style='color:#6D1B3A;'>Détail des réponses :</h3>" +
                            "<pre style='background:#f8f5f7;padding:16px;border-radius:8px;" +
                            "font-family:Arial;font-size:13px;'>" + details + "</pre>" +
                            "<p style='color:#888;font-size:12px;margin-top:20px;'>" +
                            "VitaSanté - Plateforme de Quiz Médical</p>" +
                            "</div></body></html>";

            message.setContent(contenu, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email envoyé à " + toEmail);

        } catch (Exception e) {
            System.out.println("❌ Erreur email : " + e.getMessage());
            e.printStackTrace();
        }
    }
}