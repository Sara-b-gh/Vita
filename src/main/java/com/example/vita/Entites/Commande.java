package Entites;

import com.example.vita.Entites.LigneCommande;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Commande {

    private int id_commande;
    private LocalDateTime date_commande;
    private String statut;
    private double montant_total;
    private String mode_paiement;
    private String reference_paiement;
    private List<LigneCommande> lignes = new ArrayList<>();

    public Commande() {}

    public Commande(String statut, double montant_total, String mode_paiement) {
        this.statut         = statut;
        this.montant_total  = montant_total;
        this.mode_paiement  = mode_paiement;
        this.date_commande  = LocalDateTime.now();
    }

    public int getId_commande()                        { return id_commande; }
    public void setId_commande(int id)                 { this.id_commande = id; }
    public LocalDateTime getDate_commande()            { return date_commande; }
    public void setDate_commande(LocalDateTime d)      { this.date_commande = d; }
    public String getStatut()                          { return statut; }
    public void setStatut(String statut)               { this.statut = statut; }
    public double getMontant_total()                   { return montant_total; }
    public void setMontant_total(double m)             { this.montant_total = m; }
    public String getMode_paiement()                   { return mode_paiement; }
    public void setMode_paiement(String m)             { this.mode_paiement = m; }
    public String getReference_paiement()              { return reference_paiement; }
    public void setReference_paiement(String r)        { this.reference_paiement = r; }
    public List<LigneCommande> getLignes()             { return lignes; }
    public void setLignes(List<LigneCommande> lignes)  { this.lignes = lignes; }

    @Override
    public String toString() {
        return "Commande #" + id_commande + " | " + statut
                + " | " + String.format("%.2f", montant_total) + " DT";
    }
}