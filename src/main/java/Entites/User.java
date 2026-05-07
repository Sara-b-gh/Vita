package Entites;

import java.time.LocalDate;
import java.util.Date;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private int numtel;
    private Roles role;
    private LocalDate dateNaissance;

    //tables filles
    private String departement; // DOCTOR
    private String bloodType;   // PATIENT

    public enum Roles {
        ADMIN,
        PATIENT,
        DOCTOR
    }

    public User(){}

    public User(int id, String nom, String prenom, String email, String password, int numtel, Roles role) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.numtel = numtel;
        this.role = role;
    }

    public User(String nom, String prenom, String email, String password, int numtel, Roles role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.numtel = numtel;
        this.role = role;
    }

    // Controle de saisie

    public static boolean isValidEmail(String email) {
        if (email == null) return false;

        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public static boolean isValidPhone(int numtel) {
        String phone = String.valueOf(numtel);
        return phone.matches("\\d{8}");
    }


    // getters & setters


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(" Email invalide !");
        }
        this.email = email;
    }

    public int getNumtel() {
        return numtel;
    }

    public void setNumtel(int numtel) {
        if (!isValidPhone(numtel)) {
            throw new IllegalArgumentException(" Numéro de téléphone invalide !");
        }
        this.numtel = numtel;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Roles getRole() {
        return role;
    }

    public void setRole(Roles role) {
        this.role = role;
    }

    public String getDepartement() { return departement;}

    public void setDepartement(String departement) { this.departement = departement;}

    public String getBloodType() { return bloodType;}

    public void setBloodType(String bloodType) { this.bloodType = bloodType;}

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", numtel=" + numtel +
                ", role=" + role +
                ", dateNaissance=" + dateNaissance +
                ", departement='" + departement + '\'' +
                ", bloodType='" + bloodType + '\'' +
                '}';
    }
}