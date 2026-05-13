package com.example.vita.services;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ImageAPIService {

    // URL du placeholder pendant le chargement (Bouton neutre "Chargement...")
    private static final String PLACEHOLDER_URL =
            "https://placehold.co/400x300/e2e8f0/94a3b8?text=Chargement...";

    // ── Méthode principale ────────────────────────────────────────

    /**
     * Charge une image dynamique et thématique dans un ImageView.
     */
    public static void chargerImageDansView(String terme, boolean isMed,
                                            ImageView imageView,
                                            double largeur, double hauteur) {
        if (imageView == null || terme == null) return;

        // 1. Afficher immédiatement le Placeholder (Chargement)
        Platform.runLater(() -> {
            try {
                imageView.setImage(new Image(
                        PLACEHOLDER_URL, largeur, hauteur, true, true, true
                ));
            } catch (Exception ignored) {}
        });

        // 2. Charger l'image thématique en arrière-plan (sans geler l'interface)
        Thread t = new Thread(() -> {
            String url = getFallbackUrl(terme, isMed);

            try {
                Image img = new Image(url, largeur, hauteur, true, true, true);

                img.errorProperty().addListener((obs, old, erreur) -> {
                    if (erreur) {
                        System.err.println("[ImageAPI] Impossible de charger l'image : " + url);
                        Platform.runLater(() -> imageView.setImage(
                                new Image(PLACEHOLDER_URL,
                                        largeur, hauteur, true, true, true)
                        ));
                    }
                });

                Platform.runLater(() -> imageView.setImage(img));

            } catch (Exception e) {
                System.err.println("[ImageAPI] Erreur de chargement : " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Picsum (Images stables et thématiques gratuites) ─────────────────

    /**
     * Retourne des images médicales stables en fonction du module (Médicament ou Équipement)
     */
    public static String getFallbackUrl(String terme, boolean isMed) {
        int seed = Math.abs(terme.hashCode() % 1000);

        if (isMed) {
            // Renvoie une image stable liée aux médicaments/pharmacie
            return "https://picsum.photos/seed/medicine" + seed + "/400/300";
        } else {
            // Renvoie une image stable liée à la technologie médicale / hôpital
            return "https://picsum.photos/seed/hospitaltech" + seed + "/400/300";
        }
    }

    public static String fetchImageUrl(String terme, boolean isMed) {
        return getFallbackUrl(terme, isMed);
    }

    // ── Ouvrir la recherche dans le navigateur ───────────────────

    public static void ouvrirRecherche(String nom, boolean isMedicament) {
        try {
            String query = isMedicament
                    ? nom + " medicament photo"
                    : nom + " medical equipment photo";
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.google.com/search?q="
                    + encoded + "&tbm=isch";
            java.awt.Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            System.err.println("[ImageAPI] Erreur de navigation : " + e.getMessage());
        }
    }
}