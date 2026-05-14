package services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Service utilitaire pour lire les clés API depuis .
 */
public class Configservice {

    private static final Properties props = new Properties();

    static {
        try (InputStream is = Configservice.class.getResourceAsStream("/Config.PROPERTIES")) {
            if (is != null) {
                props.load(is);
                System.out.println("[ConfigService] Fichier Config.PROPERTIES chargé avec succès !");
            } else {
                System.err.println("[ConfigService] CRITIQUE : Config.PROPERTIES introuvable dans src/main/resources/ !");
            }
        } catch (IOException e) {
            System.err.println("[ConfigService] Erreur lors de la lecture du fichier de configuration : " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "");
    }

    public static boolean isConfigured(String key) {
        String val = get(key);
        return val != null && !val.isBlank() && !val.startsWith("VOTRE_");
    }
}