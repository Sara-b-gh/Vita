javapackage com.example.vita.entities;

import java.sql.Date;
import java.time.LocalDateTime;
public class Equipement {

    private int id_equipement;
    private String nom;
    private String description;
    private String type;
    private String marque;
    private String etat; // "disponible", "en_maintenance", "hors_service"
    private String localisation;
    private Date date_acquisition;
    private LocalDateTime date_modification;

    // Constructeur vide
    public Equipement() {}

    // Constructeur complet
    public Equipement(int id_equipement, String nom, String description, String type,
                      String marque, String etat, String localisation,
                      Date date_acquisition, LocalDateTime date_modification) {
        this.id_equipement = id_equipement;
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.marque = marque;
        this.etat = etat;
        this.localisation = localisation;
        this.date_acquisition = date_acquisition;
        this.date_modification = date_modification;
    }

    // Constructeur sans id (ajout nouveau)
    public Equipement(String nom, String description, String type,
                      String marque, String etat, String localisation,
                      Date date_acquisition) {
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.marque = marque;
        this.etat = etat;
        this.localisation = localisation;
        this.date_acquisition = date_acquisition;
    }

    // Getters & Setters
    public int getId_equipement() { return id_equipement; }
    public void setId_equipement(int id_equipement) { this.id_equipement = id_equipement; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public Date getDate_acquisition() { return date_acquisition; }
    public void setDate_acquisition(Date date_acquisition) { this.date_acquisition = date_acquisition; }

    public LocalDateTime getDate_modification() { return date_modification; }
    public void setDate_modification(LocalDateTime date_modification) { this.date_modification = date_modification; }

    @Override
    public String toString() {
        return "Equipement{" +
                "id=" + id_equipement +
                ", nom='" + nom + '\'' +
                ", type='" + type + '\'' +
                ", marque='" + marque + '\'' +
                ", etat='" + etat + '\'' +
                ", localisation='" + localisation + '\'' +
                '}';
    }
}
