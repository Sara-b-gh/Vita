package Entites;

import java.time.LocalDateTime;

public class RendezVous {
    private int id_rdv;
    private int patient_id;
    private int medecin_id;
    private LocalDateTime date_rdv;
    private String motif;
    private String statut;
    private String lieu;
    private String notes;
    private LocalDateTime date_creation;
    private LocalDateTime date_modification;

    public RendezVous() {}

    public RendezVous(int patient_id, int medecin_id, LocalDateTime date_rdv,
                      String motif, String statut, String lieu, String notes) {
        this.patient_id = patient_id;
        this.medecin_id = medecin_id;
        this.date_rdv = date_rdv;
        this.motif = motif;
        this.statut = statut;
        this.lieu = lieu;
        this.notes = notes;
    }

    public int getId_rdv() { return id_rdv; }
    public void setId_rdv(int id_rdv) { this.id_rdv = id_rdv; }

    public int getPatient_id() { return patient_id; }
    public void setPatient_id(int patient_id) { this.patient_id = patient_id; }

    public int getMedecin_id() { return medecin_id; }
    public void setMedecin_id(int medecin_id) { this.medecin_id = medecin_id; }

    public LocalDateTime getDate_rdv() { return date_rdv; }
    public void setDate_rdv(LocalDateTime date_rdv) { this.date_rdv = date_rdv; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDate_creation() { return date_creation; }
    public void setDate_creation(LocalDateTime date_creation) { this.date_creation = date_creation; }

    public LocalDateTime getDate_modification() { return date_modification; }
    public void setDate_modification(LocalDateTime date_modification) { this.date_modification = date_modification; }

    @Override
    public String toString() {
        return "RendezVous{" +
                "id_rdv=" + id_rdv +
                ", patient_id=" + patient_id +
                ", medecin_id=" + medecin_id +
                ", date_rdv=" + date_rdv +
                ", motif='" + motif + '\'' +
                ", statut='" + statut + '\'' +
                ", lieu='" + lieu + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}