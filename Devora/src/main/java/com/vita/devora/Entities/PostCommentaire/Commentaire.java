package com.vita.devora.Entities;

import java.time.LocalDateTime;

public class Commentaire {

    public enum Statut{VISIBLE, MASQUE, SUPPRIME}
    private int idCommentaire;
    private int idPost;
    private int idUser;
    private String username;
    private String userRole;
    private String contenu;
    private Statut statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Integer parentId; // ⚠️ Integer (peut être NULL)

    public Commentaire() {}

    public Commentaire(int idCommentaire, int idPost, int idUser, String contenu, Statut statut, LocalDateTime dateCreation, LocalDateTime dateModification, Integer parentId) {
        this.idCommentaire = idCommentaire;
        this.idPost = idPost;
        this.idUser = idUser;
        this.contenu = contenu;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.parentId = parentId;
    }

    public Commentaire(int idPost, int idUser, String contenu, Statut statut, LocalDateTime dateCreation, LocalDateTime dateModification, Integer parentId) {
        this.idPost = idPost;
        this.idUser = idUser;
        this.contenu = contenu;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.parentId = parentId;
    }

    public int getIdCommentaire() {
        return idCommentaire;
    }

    public void setIdCommentaire(int idCommentaire) {
        this.idCommentaire = idCommentaire;
    }

    public int getIdPost() {
        return idPost;
    }

    public void setIdPost(int idPost) {
        this.idPost = idPost;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "idCommentaire=" + idCommentaire +
                ", idPost=" + idPost +
                ", idUser=" + idUser +
                ", contenu='" + contenu + '\'' +
                ", statut=" + statut +
                ", dateCreation=" + dateCreation +
                ", dateModification=" + dateModification +
                ", parentId=" + parentId +
                '}';
    }
}