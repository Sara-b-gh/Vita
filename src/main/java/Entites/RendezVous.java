package Entites;

import java.time.LocalDateTime;

public class RendezVous {
    private int id;
    private LocalDateTime date;
    private String motif;
    private String statut;
    private int medecin_id;

    public RendezVous() {
    }

    public RendezVous(LocalDateTime date, String motif, String statut, int medecin_id) {
        this.date = date;
        this.motif = motif;
        this.statut = statut;
        this.medecin_id = medecin_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getMedecin_id() {
        return medecin_id;
    }

    public void setMedecin_id(int medecin_id) {
        this.medecin_id = medecin_id;
    }

    @Override
    public String toString() {
        return "RendezVous{id=" + id + ", date=" + date + ", motif='" + motif + "', statut='" + statut + "', medecin_id=" + medecin_id + "}";
    }
}