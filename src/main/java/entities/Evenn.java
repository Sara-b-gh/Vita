package entities;
import java.time.LocalDateTime;
public class Evenn {
    private int id_Evenn;
    private int idRessource;
    private String titre;
    private LocalDateTime dateEvenement;
    private String description;
    private String lieu;
    public Evenn() {
    }
    public Evenn(int idRessource, String titre, LocalDateTime dateEvenement, String description, String lieu) {
        this.idRessource = idRessource;
        this.titre = titre;
        this.dateEvenement = dateEvenement;
        this.description = description;
        this.lieu = lieu;
    }
    public Evenn(int id_Evenn, int idRessource, String titre, LocalDateTime dateEvenement, String description, String lieu) {
        this.id_Evenn = id_Evenn;
        this.idRessource = idRessource;
        this.titre = titre;
        this.dateEvenement = dateEvenement;
        this.description = description;
        this.lieu = lieu;
    }
    public int getId_Evenn() {
        return id_Evenn;
    }
    public void setId_Evenn(int id_Evenn) {
        this.id_Evenn = id_Evenn;
    }
    public int getIdRessource() {
        return idRessource;
    }
    public void setIdRessource(int idRessource) {
        this.idRessource = idRessource;
    }
    public String getTitre() {
        return titre;
    }
    public void setTitre(String titre) {
        this.titre = titre;
    }
    public LocalDateTime getDateEvenement() {
        return dateEvenement;
    }
    public void setDateEvenement(LocalDateTime dateEvenement) {
        this.dateEvenement = dateEvenement;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getLieu() {
        return lieu;
    }
    public void setLieu(String lieu) {
        this.lieu = lieu;
    }
    @Override
    public String toString() {
        return "Evenn{" +
                "id_Evenn=" + id_Evenn +
                ", idRessource=" + idRessource +
                ", titre='" + titre + '\'' +
                ", dateEvenement=" + dateEvenement +
                ", description='" + description + '\'' +
                ", lieu='" + lieu + '\'' +
                '}';
    }
}