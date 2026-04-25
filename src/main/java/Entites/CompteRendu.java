package Entites;
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
}