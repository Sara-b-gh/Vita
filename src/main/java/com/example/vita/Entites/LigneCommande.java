package Entites;

public class LigneCommande {

    private int id_ligne;
    private int id_commande;
    private int id_medicament;
    private int id_equipement;
    private int quantite;
    private double prix_unitaire;
    private String nom_article; // nom affiché

    public LigneCommande() {}

    public LigneCommande(int id_commande, int id_medicament,
                         int id_equipement, int quantite,
                         double prix_unitaire, String nom_article) {
        this.id_commande    = id_commande;
        this.id_medicament  = id_medicament;
        this.id_equipement  = id_equipement;
        this.quantite       = quantite;
        this.prix_unitaire  = prix_unitaire;
        this.nom_article    = nom_article;
    }

    public int getId_ligne()                { return id_ligne; }
    public void setId_ligne(int i)          { this.id_ligne = i; }
    public int getId_commande()             { return id_commande; }
    public void setId_commande(int i)       { this.id_commande = i; }
    public int getId_medicament()           { return id_medicament; }
    public void setId_medicament(int i)     { this.id_medicament = i; }
    public int getId_equipement()           { return id_equipement; }
    public void setId_equipement(int i)     { this.id_equipement = i; }
    public int getQuantite()                { return quantite; }
    public void setQuantite(int q)          { this.quantite = q; }
    public double getPrix_unitaire()        { return prix_unitaire; }
    public void setPrix_unitaire(double p)  { this.prix_unitaire = p; }
    public String getNom_article()          { return nom_article; }
    public void setNom_article(String n)    { this.nom_article = n; }

    public double getSousTotal() {
        return quantite * prix_unitaire;
    }

    @Override
    public String toString() {
        return nom_article + " x" + quantite
                + " = " + String.format("%.2f", getSousTotal()) + " DT";
    }
}