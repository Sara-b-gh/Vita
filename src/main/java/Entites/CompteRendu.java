/*package Entites;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class CompteRendu {
    private int id;
    private int rdv_id;
    private String contenu;
    private LocalDate dateRedaction;

    public CompteRendu() {}

    public CompteRendu(int rdv_id, String contenu, LocalDate dateRedaction) {
        this.rdv_id = rdv_id;
        this.contenu = contenu;
        this.dateRedaction = dateRedaction;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRdv_id() { return rdv_id; }
    public void setRdv_id(int rdv_id) { this.rdv_id = rdv_id; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDate getDateRedaction() { return dateRedaction; }
    public void setDateRedaction(LocalDate d) { this.dateRedaction = d; }

    @Override
    public String toString() {
        return "CompteRendu{id=" + id + ", rdv_id=" + rdv_id + ", date=" + dateRedaction + "}";
    }
}*/
package Entites;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CompteRendu {
    private int id_cr;
    private int id_rdv;
    private int redige_par;
    private String contenu;
    private String diagnostic;
    private String traitement;
    private LocalDate prochain_rdv;
    private boolean confidentiel;
    private LocalDateTime date_creation;
    private LocalDateTime date_modification;

    public CompteRendu() {}

    public CompteRendu(int id_rdv, int redige_par, String contenu, String diagnostic,
                       String traitement, LocalDate prochain_rdv, boolean confidentiel) {
        this.id_rdv = id_rdv;
        this.redige_par = redige_par;
        this.contenu = contenu;
        this.diagnostic = diagnostic;
        this.traitement = traitement;
        this.prochain_rdv = prochain_rdv;
        this.confidentiel = confidentiel;
    }

    // Getters & Setters
    public int getId_cr() { return id_cr; }
    public void setId_cr(int id_cr) { this.id_cr = id_cr; }

    public int getId_rdv() { return id_rdv; }
    public void setId_rdv(int id_rdv) { this.id_rdv = id_rdv; }

    public int getRedige_par() { return redige_par; }
    public void setRedige_par(int redige_par) { this.redige_par = redige_par; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getDiagnostic() { return diagnostic; }
    public void setDiagnostic(String diagnostic) { this.diagnostic = diagnostic; }

    public String getTraitement() { return traitement; }
    public void setTraitement(String traitement) { this.traitement = traitement; }

    public LocalDate getProchain_rdv() { return prochain_rdv; }
    public void setProchain_rdv(LocalDate prochain_rdv) { this.prochain_rdv = prochain_rdv; }

    public boolean isConfidentiel() { return confidentiel; }
    public void setConfidentiel(boolean confidentiel) { this.confidentiel = confidentiel; }

    public LocalDateTime getDate_creation() { return date_creation; }
    public void setDate_creation(LocalDateTime date_creation) { this.date_creation = date_creation; }

    public LocalDateTime getDate_modification() { return date_modification; }
    public void setDate_modification(LocalDateTime date_modification) { this.date_modification = date_modification; }

    @Override
    public String toString() {
        return "CompteRendu{id_cr=" + id_cr + ", id_rdv=" + id_rdv +
                ", redige_par=" + redige_par + ", confidentiel=" + confidentiel +
                ", date_creation=" + date_creation + "}";
    }
}