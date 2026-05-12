package Entites;

import java.time.LocalDate;

public class User {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private int phoneNumber;
    private Roles role;
    private LocalDate birthDate;
    private String department;
    private String bloodType;

    public enum Roles {
        ADMIN,
        PATIENT,
        DOCTOR
    }

    // ── Constructeur ──────────────────────────────────────────────────

    public User(int id, String nom, String prenom, String email, String password, int phoneNumber, Roles role) {
        this.id          = id;
        this.nom         = nom;
        this.prenom      = prenom;
        this.email       = email;
        this.password    = password;
        this.phoneNumber = phoneNumber;
        this.role        = role;
    }

    // ── Validation ────────────────────────────────────────────────────

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public static boolean isValidPhone(int phoneNumber) {
        return String.valueOf(phoneNumber).matches("\\d{8}");
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (!isValidEmail(email)) throw new IllegalArgumentException("Email invalide !");
        this.email = email;
    }

    public int getPhoneNumber() { return phoneNumber; }

    public String getTelephone() { return String.valueOf(phoneNumber); }

    public void setPhoneNumber(int phoneNumber) {
        if (!isValidPhone(phoneNumber)) throw new IllegalArgumentException("Numéro de téléphone invalide !");
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Roles getRole() { return role; }
    public void setRole(Roles role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    // ── toString pour ComboBox ────────────────────────────────────────
    @Override
    public String toString() {
        return "Dr " + nom + " " + prenom;
    }
}