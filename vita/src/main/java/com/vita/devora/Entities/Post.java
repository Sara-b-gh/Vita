package com.vita.devora.Entities;

import java.time.LocalDateTime;

public class Post {

    public enum Statut{ACTIF, ARCHIVE}
    public enum Category{QUESTION, STORY, OPPINION,ADVICE };
    private int idPost;
    private int idUser;
    private Category category;
    private String contenu;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Statut statut;
    private int nbCommentaire;


    public Post() {
    }

    public Post(int idPost, int idUser, Category category, String contenu, LocalDateTime dateCreation, LocalDateTime dateModification, Statut statut, int nbCommentaire) {
        this.idPost = idPost;
        this.idUser = idUser;
        this.category = category;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.statut = statut;
        this.nbCommentaire = nbCommentaire;
    }

    public Post(int idUser, Category category, String contenu, LocalDateTime dateCreation, LocalDateTime dateModification, Statut statut, int nbCommentaire) {
        this.idUser = idUser;
        this.category = category;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.statut = statut;
        this.nbCommentaire = nbCommentaire;
    }


//    public int getIdPost() {
//        return idPost;
//    }
//
//    public void setIdPost(int idPost) {
//        this.idPost = idPost;
//    }


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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
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

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public int getNbCommentaire() {
        return nbCommentaire;
    }

    public void setNbCommentaire(int nbCommentaire) {
        this.nbCommentaire = nbCommentaire;
    }

    @Override
    public String toString() {
        return "Post{" +
                "idPost=" + idPost +
                ", idUser=" + idUser +
                ", category=" + category +
                ", contenu='" + contenu + '\'' +
                ", dateCreation=" + dateCreation +
                ", dateModification=" + dateModification +
                ", statut=" + statut +
                ", nbCommentaire=" + nbCommentaire +
                '}';
    }

}