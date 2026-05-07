package Entites;

import java.time.LocalDate;
import java.time.LocalTime;

public class Disponibilite {
    private int id_dispo;
    private int medecin_id;
    private LocalDate date_dispo;
    private LocalTime heure_debut;
    private LocalTime heure_fin;
    private String statut; // "libre", "occupee", "bloquee"
    private Integer id_rdv; // nullable

    public Disponibilite() {}

    public Disponibilite(int medecin_id, LocalDate date_dispo,
                         LocalTime heure_debut, LocalTime heure_fin, String statut) {
        this.medecin_id  = medecin_id;
        this.date_dispo  = date_dispo;
        this.heure_debut = heure_debut;
        this.heure_fin   = heure_fin;
        this.statut      = statut;
    }

    // ── Getters / Setters ──────────────────────────────────────────────
    public int getId_dispo()                        { return id_dispo; }
    public void setId_dispo(int id_dispo)           { this.id_dispo = id_dispo; }

    public int getMedecin_id()                      { return medecin_id; }
    public void setMedecin_id(int medecin_id)       { this.medecin_id = medecin_id; }

    public LocalDate getDate_dispo()                { return date_dispo; }
    public void setDate_dispo(LocalDate date_dispo) { this.date_dispo = date_dispo; }

    public LocalTime getHeure_debut()               { return heure_debut; }
    public void setHeure_debut(LocalTime h)         { this.heure_debut = h; }

    public LocalTime getHeure_fin()                 { return heure_fin; }
    public void setHeure_fin(LocalTime h)           { this.heure_fin = h; }

    public String getStatut()                       { return statut; }
    public void setStatut(String statut)            { this.statut = statut; }

    public Integer getId_rdv()                      { return id_rdv; }
    public void setId_rdv(Integer id_rdv)           { this.id_rdv = id_rdv; }

    @Override
    public String toString() {
        return heure_debut + " – " + heure_fin + " [" + statut + "]";
    }
}
