package Services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class SmsService {

    private static final String EMAIL_FROM = "tabainour30@gmail.com";
    private static final String EMAIL_PASS = "kxms vtvr hylc amrr"; // ← coller ici sans espaces

    public static void envoyerNotification(String emailDestinataire,
                                           String sujet,
                                           String texte) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASS);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(EMAIL_FROM, "VITA Hôpital"));
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(emailDestinataire));
            msg.setSubject(sujet);
            msg.setText(texte);
            Transport.send(msg);
            System.out.println("✅ Email envoyé à " + emailDestinataire);

        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }
}