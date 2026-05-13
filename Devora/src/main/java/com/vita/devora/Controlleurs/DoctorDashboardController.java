package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.NewsArticle;
import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.NewsService;
import com.vita.devora.utils.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;

public class DoctorDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private Button dashBtn;
    @FXML private Button mesPatientsBtn;
    @FXML private FlowPane patientCardsPane;
    @FXML private VBox newsContainer;
    @FXML private Label loadingLabel;
    @FXML private TextField newsSearchField;

    private final UserService userService = new UserService();
    private List<User> patientList;

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() != null) {
            welcomeLabel.setText("Bienvenue Dr. " + SessionManager.getCurrentUser().getNom());
        }

        loadMyPatients();
        loadNews();

        // Recherche avec touche Entrée
        newsSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleNewsSearch(null);
            }
        });
    }

    // ══════════════════════════════════════
    // PATIENTS
    // ══════════════════════════════════════
    private void loadMyPatients() {
        User doctor = SessionManager.getCurrentUser();
        if (doctor == null) return;
        try {
            patientList = userService.getPatientsByDoctor(doctor.getId());
            totalPatientsLabel.setText(String.valueOf(patientList.size()));
            buildCards(patientList);
        } catch (SQLException e) {
            totalPatientsLabel.setText("0");
            e.printStackTrace();
        }
    }

    private void buildCards(List<User> patients) {
        patientCardsPane.getChildren().clear();
        for (User u : patients) {
            patientCardsPane.getChildren().add(createCard(u));
        }
    }

    private VBox createCard(User u) {
        Label avatar = new Label(
                (u.getPrenom().charAt(0) + "" + u.getNom().charAt(0)).toUpperCase()
        );
        avatar.setStyle("""
            -fx-background-color: #fdedf3;
            -fx-text-fill: #8a0037;
            -fx-font-weight: bold;
            -fx-font-size: 16;
        """);

        StackPane avatarBox = new StackPane(avatar);
        avatarBox.setMinSize(50, 50);
        avatarBox.setStyle("-fx-background-radius: 25; -fx-background-color: #fdedf3;");

        Label name = new Label(u.getPrenom() + " " + u.getNom());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label bloodType = new Label(u.getBloodType() != null ? u.getBloodType() : "Non assigné");
        bloodType.setStyle("""
            -fx-background-color: #ffe4e1;
            -fx-text-fill: #d32f2f;
            -fx-background-radius: 12;
            -fx-padding: 2 10;
            -fx-font-size: 11;
        """);

        VBox info = new VBox(4, name, bloodType);
        HBox header = new HBox(10, avatarBox, info);

        Label email = new Label("✉ " + u.getEmail());
        Label tel = new Label("☎ " + u.getNumtel());
        VBox body = new VBox(6, email, tel);

        VBox card = new VBox(10, header, body);
        card.getStyleClass().add("vita-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(240);
        card.setMaxWidth(240);

        return card;
    }

    // ══════════════════════════════════════
    // NEWS
    // ══════════════════════════════════════
    private void loadNews() {
        loadingLabel.setText("⏳ Chargement des actualités...");
        newsContainer.getChildren().clear();

        Thread thread = new Thread(() -> {
            try {
                List<NewsArticle> articles = NewsService.fetchMedicalNews();

                Platform.runLater(() -> {
                    loadingLabel.setText("");
                    newsContainer.getChildren().clear();

                    if (articles.isEmpty()) {
                        Label noNews = new Label("Aucune actualité disponible.");
                        noNews.setStyle("-fx-font-size: 14; -fx-text-fill: #636E72;");
                        newsContainer.getChildren().add(noNews);
                        return;
                    }

                    for (NewsArticle article : articles) {
                        newsContainer.getChildren().add(createNewsCard(article));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                        loadingLabel.setText("❌ Impossible de charger les actualités.")
                );
                e.printStackTrace();
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleNewsSearch(ActionEvent actionEvent) {
        String query = newsSearchField.getText().trim();
        if (query.isEmpty()) {
            loadNews();
            return;
        }

        loadingLabel.setText("⏳ Recherche en cours...");
        newsContainer.getChildren().clear();

        Thread thread = new Thread(() -> {
            try {
                List<NewsArticle> articles = NewsService.searchNews(query);
                Platform.runLater(() -> {
                    loadingLabel.setText(articles.size() + " résultat(s) pour : " + query);
                    newsContainer.getChildren().clear();

                    if (articles.isEmpty()) {
                        Label noNews = new Label("❌ Aucun résultat pour : " + query);
                        noNews.setStyle("-fx-font-size: 14; -fx-text-fill: #636E72;");
                        newsContainer.getChildren().add(noNews);
                        return;
                    }

                    for (NewsArticle article : articles) {
                        newsContainer.getChildren().add(createNewsCard(article));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        loadingLabel.setText("❌ Erreur de recherche.")
                );
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleNewsReset(ActionEvent actionEvent) {
        newsSearchField.clear();
        loadingLabel.setText("");
        loadNews();
    }

    private VBox createNewsCard(NewsArticle article) {
        Label title = new Label(article.getTitle());
        title.setStyle("""
            -fx-font-weight: bold;
            -fx-font-size: 14;
            -fx-text-fill: #8a0037;
            -fx-wrap-text: true;
        """);
        title.setMaxWidth(900);

        String date = article.getPublishedAt() != null
                ? article.getPublishedAt().substring(0, 10) : "";
        Label meta = new Label("📰 " + article.getSourceName() + "  •  📅 " + date);
        meta.setStyle("-fx-font-size: 11; -fx-text-fill: #636E72;");

        Label desc = new Label(
                article.getDescription() != null ? article.getDescription() : "Pas de description."
        );
        desc.setStyle("""
            -fx-font-size: 12;
            -fx-text-fill: #444;
            -fx-wrap-text: true;
        """);
        desc.setMaxWidth(900);

        Hyperlink link = new Hyperlink("🔗 Lire l'article complet");
        link.setStyle("-fx-text-fill: #185FA5; -fx-font-size: 12;");
        link.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI(article.getUrl()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        VBox card = new VBox(6, title, meta, desc, link);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 10;
            -fx-border-color: #e8e8e8;
            -fx-border-radius: 10;
            -fx-border-width: 1;
            -fx-padding: 12;
        """);
        card.setPadding(new Insets(12));

        return card;
    }

    // ══════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════
    @FXML
    private void haddleDashbordDocteur(ActionEvent actionEvent) {
        // déjà sur cette page
    }

    @FXML
    private void handleProfile(ActionEvent actionEvent) {
        switchPage(actionEvent, "/com/vita/devora/DocteurPassword.fxml");
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/LoginTest.fxml"));
            Stage stage = (Stage) patientCardsPane.getScene().getWindow();

            // ← REBLOQUER pour le login
            stage.setResizable(false);
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.centerOnScreen();

            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void switchPage(ActionEvent event, String fxmlPath) {
        try {
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("❌ FXML File not found at: " + fxmlPath);
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();
            javafx.scene.Node sourceNode = (javafx.scene.Node) event.getSource();
            javafx.stage.Stage stage = (javafx.stage.Stage) sourceNode.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            System.err.println("❌ Critical error loading: " + fxmlPath);
            e.printStackTrace();
        }
    }
}