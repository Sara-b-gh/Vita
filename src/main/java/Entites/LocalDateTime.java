package Entites;

import java.time.Duration;

public class LocalDateTime {
    public static class Notification {
        private int id;
        private String type;       // "alert", "info", "calendar"
        private String titre;
        private String message;
        private java.time.LocalDateTime dateCreation;  // 👈 changé : createdAt → dateCreation
        private int isRead;                  // 👈 changé : read → isRead (0 = non lu, 1 = lu)

        public Notification() {}

        public Notification(int id, String type, String titre, String message,
                            java.time.LocalDateTime dateCreation, Boolean isRead) {
            this.id = id;
            this.type = type;
            this.titre = titre;
            this.message = message;
            this.dateCreation = dateCreation;
            this.isRead = isRead ? 1 : 0;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public java.time.LocalDateTime getDateCreation() { return dateCreation; }  // 👈 changé
        public void setDateCreation(java.time.LocalDateTime dateCreation) { this.dateCreation = dateCreation; }  // 👈 changé

        public int getIsRead() { return isRead; }  // 👈 changé
        public void setIsRead(int isRead) { this.isRead = isRead; }  // 👈 changé

        // Méthode utilitaire pour vérifier si lu (plus pratique)
        public boolean isRead() {
            return isRead == 1;
        }

        public void setRead(boolean read) {
            this.isRead = read ? 1 : 0;
        }

        /** Retourne "Il y a 10 min", "Il y a 2 heures", etc. */
        public String getTimeAgo() {
            if (dateCreation == null) return "";
            long minutes = Duration.between(dateCreation, java.time.LocalDateTime.now()).toMinutes();
            if (minutes < 1)  return "à l'instant";
            if (minutes < 60) return "Il y a " + minutes + " min";
            long heures = minutes / 60;
            if (heures < 24)  return "Il y a " + heures + " heure" + (heures > 1 ? "s" : "");
            long jours = heures / 24;
            return "Il y a " + jours + " jour" + (jours > 1 ? "s" : "");
        }
    }
}
