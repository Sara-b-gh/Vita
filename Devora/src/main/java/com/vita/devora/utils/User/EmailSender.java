package com.vita.devora.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Envoi d'email via Gmail SMTP (JavaMail).
 *
 * Configuration requise :
 *  1. Ajouter dans pom.xml :
 *       <dependency>
 *           <groupId>com.sun.mail</groupId>
 *           <artifactId>javax.mail</artifactId>
 *           <version>1.6.2</version>
 *       </dependency>
 *
 *  2. Créer un mot de passe d'application Gmail :
 *       myaccount.google.com → Sécurité → Mots de passe d'application
 *
 *  3. Remplir EMAIL_EXPEDITEUR et APP_PASSWORD ci-dessous.
 */
public class EmailSender {

    // ⚠️ À modifier avec ton compte Gmail
    private static final String EMAIL_EXPEDITEUR = "gherrisarah@gmail.com";
    private static final String APP_PASSWORD      = "hxwi ftoj kvys vybo";

    /**
     * Envoie les identifiants de connexion à un nouvel utilisateur.
     *
     * @param emailDestinataire  email du médecin ou patient
     * @param nomComplet         ex : "Yasmine Trabelsi"
     * @param role               ex : "Médecin" ou "Patient"
     * @param motDePasse         mot de passe généré automatiquement
     */
    public static void envoyerCredentials(String emailDestinataire,
                                          String nomComplet,
                                          String role,
                                          String motDePasse) {
        // 1. Paramètres SMTP Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        // 2. Session avec authentification
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_EXPEDITEUR, APP_PASSWORD);
            }
        });

        try {
            // 3. Construire le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_EXPEDITEUR, "Gestion Hospitalière"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(emailDestinataire));
            message.setSubject("🏥 Vos identifiants – Système de Gestion Hospitalière");
            message.setContent(construireCorpsEmail(nomComplet, role,
                    emailDestinataire, motDePasse), "text/html; charset=utf-8");

            // 4. Envoyer
            Transport.send(message);
            System.out.println("✅ Email envoyé à : " + emailDestinataire);

        } catch (Exception e) {
            System.out.println("❌ Erreur envoi email : " + e.getMessage());
            throw new RuntimeException("Échec de l'envoi email", e);
        }
    }

    // Corps HTML de l'email
    private static String construireCorpsEmail(String nomComplet, String role,
                                               String email, String motDePasse) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:30px;">
                  <div style="max-width:520px; margin:auto; background:white;
                              border-radius:12px; overflow:hidden;
                              box-shadow: 0 4px 12px rgba(0,0,0,0.1);">

                    <div style="background: linear-gradient(135deg,#ff6b81,#ff4757);
                                padding:28px 32px; text-align:center;">
                      <h1 style="color:white; margin:0; font-size:22px;">🏥 Gestion Hospitalière</h1>
                      <p style="color:rgba(255,255,255,0.85); margin:6px 0 0;">Système de gestion intégré</p>
                    </div>

                    <div style="padding:32px;">
                      <p style="font-size:16px; color:#333;">Bonjour <strong>%s</strong>,</p>
                      <p style="color:#555; line-height:1.6;">
                        Votre compte <strong>%s</strong> a été créé avec succès.
                        Voici vos identifiants de connexion :
                      </p>

                      <div style="background:#fff0f2; border-left:4px solid #ff4757;
                                  border-radius:8px; padding:20px 24px; margin:20px 0;">
                        <p style="margin:0 0 10px;">
                          <span style="color:#888; font-size:13px;">ADRESSE EMAIL</span><br>
                          <strong style="font-size:15px; color:#333;">%s</strong>
                        </p>
                        <hr style="border:none; border-top:1px solid #ffd0d5; margin:12px 0;">
                        <p style="margin:0;">
                          <span style="color:#888; font-size:13px;">MOT DE PASSE</span><br>
                          <strong style="font-size:18px; color:#ff4757;
                                         letter-spacing:2px; font-family:monospace;">%s</strong>
                        </p>
                      </div>

                      <p style="color:#e84040; font-size:13px;">
                        ⚠️ Changez votre mot de passe dès votre première connexion.
                      </p>
                    </div>

                    <div style="background:#f9f9f9; padding:16px 32px;
                                text-align:center; color:#aaa; font-size:12px;">
                      Cet email a été envoyé automatiquement — ne pas répondre.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(nomComplet, role, email, motDePasse);
    }
}
