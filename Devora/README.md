# Système de Gestion Médicale

## Description

Ce projet consiste à développer une application de gestion médicale permettant de faciliter l’interaction entre patients, docteurs et administrateurs.
Le système permet la gestion des consultations, des rendez-vous, des dossiers médicaux, des utilisateurs ainsi que des événements et des commentaires.

---

## Objectifs

* Digitaliser la gestion des services médicaux
* Améliorer l’organisation des consultations
* Faciliter l’accès aux informations médicales
* Centraliser les données du système

---

## Acteurs du système

### Patient

* S’inscrire et se connecter
* Prendre un rendez-vous
* Consulter ses rendez-vous
* Consulter son dossier médical
* Consulter ses analyses
* Ajouter des commentaires
* Consulter des évènements


### Docteur

* Gérer les consultations (présentielles et en ligne)
* Rédiger des comptes rendus
* Gérer les rendez-vous
* Consulter les dossiers patients
* Suivre les analyses
* Consulter des évènements

### Administrateur

* Gérer les utilisateurs
* Gérer les patients
* Gérer les docteurs
* Gérer les médicaments
* Gérer les événements
* Gérer les commentaires

---

## Gestion des utilisateurs

Le système repose sur une entité principale appelée **Utilisateur**.

Un utilisateur peut avoir l’un des rôles suivants :

* Patient
* Docteur
* Administrateur

Après authentification, le système identifie le rôle de l’utilisateur et lui donne accès aux fonctionnalités correspondantes.

---

## Fonctionnalités principales

### Authentification

* Inscription
* Connexion

### Gestion des patients

* Ajouter un patient
* Modifier un patient
* Supprimer un patient
* Consulter les informations

### Gestion des docteurs

* Ajouter un docteur
* Modifier un docteur
* Supprimer un docteur
* Consulter les informations

### Gestion des rendez-vous

* Créer un rendez-vous
* Modifier ou annuler

### Gestion des consultations

* Consultation en ligne
* Consultation présentielle
* Rédaction de compte rendu

### Dossier médical

* Consultation des informations
* Historique médical

### Gestion des médicaments

* Ajouter
* Modifier
* Supprimer

### Gestion des événements

* Ajouter
* Modifier
* Supprimer

### Gestion des commentaires

* Ajouter
* Modifier
* Supprimer
* Signaler

---

## Architecture

Le projet suit une architecture MVC :

* Model : gestion des données
* View : interfaces graphiques (FXML avec Scene Builder)
* Controller : logique applicative

---

## Technologies utilisées

* Java
* JavaFX
* MySQL
* Scene Builder
* UML
* Git et GitHub

---

## Installation

Cloner le projet :

```bash
git clone https://github.com/Sara-b-gh/Vita.git
```
---

## Exécution

* Ouvrir le projet avec IntelliJ IDEA
* Exécuter la classe principale

---

## Améliorations futures

* Mise en place d’une API REST
* Sécurisation avancée de l’authentification
* Déploiement en application web

---

## Auteurs

Nom : Equipe Devora
Établissement : ESPRIT
Niveau : 1ère année cycle ingénieur

---

## Licence

Projet académique à usage pédagogique
