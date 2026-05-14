package Controlers;

import services.ChatbotService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.ResourceBundle;

public class Chatbotcontroller implements Initializable {

    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private Label statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ajouterMessageBot("Bonjour ! Je suis VITA Assistant 🏥\nComment puis-je vous aider ?");

        if (messageInput != null) {
            messageInput.setOnKeyPressed(e -> {
                if ("ENTER".equals(e.getCode().toString())) envoyerMessage();
            });
        }

        if (messagesContainer != null) {
            messagesContainer.heightProperty().addListener((obs, o, n) -> scrollVersLesBas());
        }
    }

    @FXML
    public void envoyerMessage() {
        if (messageInput == null) return;
        String texte = messageInput.getText().trim();
        if (texte.isBlank()) return;

        messageInput.clear();
        ajouterMessageUser(texte);
        setInputActif(false);

        if (statusLabel != null) {
            statusLabel.setText("VITA écrit...");
            statusLabel.setVisible(true);
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return ChatbotService.repondre(texte);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                ajouterMessageBot(task.getValue());
                setInputActif(true);
                if (statusLabel != null) statusLabel.setVisible(false);
                messageInput.requestFocus();
            });
        });

        task.setOnFailed(e -> {
            if (task.getException() != null) {
                task.getException().printStackTrace(); // Imprime l'erreur exacte dans l'IDE
            }
            Platform.runLater(() -> {
                ajouterMessageBot("Désolé, une erreur est survenue lors de la génération de la réponse.");
                setInputActif(true);
                if (statusLabel != null) statusLabel.setVisible(false);
            });
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void reinitialiserConversation() {
        ChatbotService.reinitialiserConversation();
        if (messagesContainer != null) messagesContainer.getChildren().clear();
        ajouterMessageBot("Conversation réinitialisée. Comment puis-je vous aider ?");
    }

    private void ajouterMessageUser(String texte) {
        HBox conteneur = new HBox();
        conteneur.setAlignment(Pos.CENTER_RIGHT);
        conteneur.setPadding(new Insets(4, 8, 4, 60));

        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(320);
        bulle.setFont(Font.font("Segoe UI", 13));
        bulle.setTextFill(Color.WHITE);
        bulle.setPadding(new Insets(10, 14, 10, 14));
        bulle.setStyle("-fx-background-color: #6b0d1e; -fx-background-radius: 18 18 4 18;");

        conteneur.getChildren().add(bulle);
        if (messagesContainer != null) messagesContainer.getChildren().add(conteneur);
    }

    private void ajouterMessageBot(String texte) {
        HBox conteneur = new HBox(8);
        conteneur.setAlignment(Pos.CENTER_LEFT);
        conteneur.setPadding(new Insets(4, 60, 4, 8));

        Label avatar = new Label("🤖");
        avatar.setFont(Font.font(20));
        avatar.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 50%; -fx-padding: 4 6;");

        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(320);
        bulle.setFont(Font.font("Segoe UI", 13));
        bulle.setTextFill(Color.web("#1a1a2e"));
        bulle.setPadding(new Insets(10, 14, 10, 14));
        bulle.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 18 18 18 4;");

        conteneur.getChildren().addAll(avatar, bulle);
        if (messagesContainer != null) messagesContainer.getChildren().add(conteneur);
    }

    @FXML public void demanderAideMedicaments() { poserQuestion("Comment gérer les médicaments ?"); }
    @FXML public void demanderAideEquipements() { poserQuestion("Comment gérer les équipements ?"); }
    @FXML public void demanderAideCommandes()   { poserQuestion("Comment passer une commande ?"); }
    @FXML public void demanderAidePaiement()    { poserQuestion("Comment fonctionne le paiement ?"); }

    private void poserQuestion(String question) {
        if (messageInput != null) {
            messageInput.setText(question);
            envoyerMessage();
        }
    }

    private void setInputActif(boolean actif) {
        if (messageInput != null) messageInput.setDisable(!actif);
        if (sendButton   != null) sendButton.setDisable(!actif);
    }

    private void scrollVersLesBas() {
        if (scrollPane != null) {
            scrollPane.layout();
            scrollPane.setVvalue(1.0);
        }
    }
}