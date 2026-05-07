package com.vita.devora.Controllers;

import com.vita.devora.Entities.*;
import com.vita.devora.Services.*;
import com.vita.devora.Utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CommunityViewController {

    @FXML private VBox postsContainer;
    @FXML private Label totalPublications, totalCommentaires, postCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ScrollPane mainScrollPane;

    private final ServicePost servicePost = new ServicePost();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private List<PostView> allPosts;

    @FXML
    public void initialize() {
        setupFilters();
        loadData();

        // Admin Search: Filters content or username
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadPosts() {
        // CRITICAL: Clear the VBox before reloading
        postsContainer.getChildren().clear();

        // Fetch the fresh list from the service
        List<PostView> posts = servicePost.getAllPostsSortedByDateDesc();

        // Update the count label
        postCountLabel.setText("Liste des publications (" + posts.size() + ")");

        // Re-build the rows
        for (PostView post : posts) {
            HBox row = createAdminRow(post);
            postsContainer.getChildren().add(row);
        }
    }

    private void setupFilters() {
        categoryFilter.getItems().add("Toutes");
        for (Post.Category cat : Post.Category.values()) {
            categoryFilter.getItems().add(cat.name());
        }
        categoryFilter.setValue("Toutes");
        categoryFilter.setOnAction(e -> applyFilters());
    }

    private void loadData() {
        allPosts = servicePost.getAllPostsSortedByDateDesc();
        updateStats();
        renderTable(allPosts);
    }

    private void updateStats() {
        try {
            totalPublications.setText(String.valueOf(allPosts.size()));
            // Count all comments across the DB
            int totalComm = serviceCommentaire.GetAllComments().size();
            totalCommentaires.setText(String.valueOf(totalComm));
            postCountLabel.setText("Liste des publications (" + allPosts.size() + ")");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        String cat = categoryFilter.getValue();

        List<PostView> filtered = allPosts.stream()
                .filter(p -> p.getContenu().toLowerCase().contains(search) || p.getUsername().toLowerCase().contains(search))
                .filter(p -> cat.equals("Toutes") || p.getCategory().equalsIgnoreCase(cat))
                .collect(Collectors.toList());

        renderTable(filtered);
    }

    private void renderTable(List<PostView> posts) {
        postsContainer.getChildren().clear();
        for (PostView post : posts) {
            postsContainer.getChildren().add(createAdminRow(post));
        }
    }

    private HBox createAdminRow(PostView post) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #f2f2f2; -fx-border-width: 0 0 1 0; -fx-padding: 15; -fx-background-color: white;");

        // 1. CONTENU (The Flexible Column)
        Label lblContent = new Label(post.getContenu());
        lblContent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lblContent, Priority.ALWAYS); // This pushes everything else to the right
        lblContent.setStyle("-fx-text-fill: #444; -fx-font-size: 13px;");

        // 2. CATÉGORIE (Fixed Width)
        Label lblCat = new Label(post.getCategory().toLowerCase());
        lblCat.setStyle("-fx-background-color: #eef2ff; -fx-text-fill: #4f46e5; -fx-background-radius: 15; -fx-padding: 4 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        StackPane catPane = new StackPane(lblCat);
        catPane.setPrefWidth(120);
        catPane.setAlignment(Pos.CENTER_LEFT);

        // 3. AUTEUR (Fixed Width)
        VBox authorBox = new VBox(2);
        authorBox.setPrefWidth(150);
        Label name = new Label(post.getUsername());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        Label role = new Label(post.getUserRole());
        role.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
        authorBox.getChildren().addAll(name, role);

        // 4. DATE (Fixed Width)
        Label lblDate = new Label(post.getDateCreation().toLocalDate().toString());
        lblDate.setPrefWidth(120);
        lblDate.setStyle("-fx-text-fill: #666;");

        // 5. INTERACTION (Fixed Width)
        Label interaction = new Label(String.valueOf(post.getNbCommentaire()));
        interaction.setPrefWidth(100);
        interaction.setStyle("-fx-font-weight: bold;");

        // 6. MODÉRER (Fixed Width)
        HBox actions = new HBox(12);
        actions.setPrefWidth(100);
        actions.setAlignment(Pos.CENTER);

        Button btnView = new Button("👁");
        styleActionButton(btnView, "#4f46e5", "#eef2ff");
        btnView.setOnAction(event -> showPostDetails(post));
        Button btnDelete = new Button("🗑");
        styleActionButton(btnDelete, "#dc2626", "#fee2e2");
        btnDelete.setOnAction(event -> handleDeletePost(post));

        actions.getChildren().addAll(btnView, btnDelete);

        // Add everything to the row
        row.getChildren().addAll(lblContent, catPane, authorBox, lblDate, interaction, actions);

        return row;
    }

    // Helper to keep code clean
    private void styleActionButton(Button btn, String color, String bgColor) {
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-min-width: 35px;" +
                        "-fx-min-height: 35px;" +
                        "-fx-max-width: 35px;" +
                        "-fx-max-height: 35px;"
        );

        // Hover animation
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-scale-x: 1.1; -fx-scale-y: 1.1;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle() + "-fx-scale-x: 1; -fx-scale-y: 1;"));
    }

    private void showPostDetails(PostView post) {
        postsContainer.getChildren().clear();

        VBox detailRoot = new VBox(15);
        detailRoot.setStyle("-fx-padding: 20; -fx-background-color: white;");

        Button backBtn = new Button("← Retour à la liste");
        backBtn.setOnAction(e -> renderTable(allPosts));

        Label title = new Label("Post de " + post.getUsername());
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Label content = new Label(post.getContenu());
        content.setWrapText(true);
        content.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 15; -fx-background-radius: 10;");

        VBox commentSection = new VBox(10);
        commentSection.getChildren().add(new Label("Modération des commentaires :"));

        // Get comments using your getByPost method
        List<Commentaire> comments = serviceCommentaire.getByPost(post.getIdPost());

        for (Commentaire c : comments) {
            HBox cRow = new HBox(10);
            cRow.setAlignment(Pos.CENTER_LEFT);
            cRow.setStyle("-fx-padding: 10; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");

            VBox textSide = new VBox(2);
            Label userLabel = new Label(c.getUsername() + " (" + c.getUserRole() + ")");
            userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
            Label cText = new Label(c.getContenu());
            textSide.getChildren().addAll(userLabel, cText);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button delComm = new Button("Supprimer");
            delComm.setStyle("-fx-background-color: #fee; -fx-text-fill: #c00; -fx-font-size: 10;");
            delComm.setOnAction(e -> {
                try {
                    serviceCommentaire.DeleteComment(c.getIdCommentaire());
                    showPostDetails(post); // Refresh this view
                } catch (SQLException ex) { ex.printStackTrace(); }
            });

            cRow.getChildren().addAll(textSide, spacer, delComm);
            commentSection.getChildren().add(cRow);

            List<Commentaire> replies = serviceCommentaire.getReplies(c.getIdCommentaire());

            System.out.println("Parent ID: " + c.getIdCommentaire() + " has " + replies.size() + " replies.");
            for (Commentaire r : replies) {
                VBox rBox = new VBox(5);
                rBox.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #e0e0e0;");
                VBox.setMargin(rBox, new Insets(0, 0, 0, 40));

                HBox rHeader = new HBox(10);
                rHeader.setAlignment(Pos.CENTER_LEFT);

                // CHANGED: Display Name instead of "ID: " string
                Label rUser = new Label(r.getUsername() != null ? r.getUsername() : "Utilisateur " + r.getIdUser());
                rUser.setStyle("-fx-font-weight: bold; -fx-font-size: 11; -fx-text-fill: #555;");

                // ADDED: Display Role Badge for replies
                String rRoleStr = (r.getUserRole() != null) ? r.getUserRole().toUpperCase() : "USER";
                Label rRoleBadge = new Label(rRoleStr);
                //rRoleBadge.setStyle(getRoleBadgeStyle(rRoleStr));
                rRoleBadge.setScaleX(0.8); rRoleBadge.setScaleY(0.8); // Smaller badge for nested replies

                Region rSpacer = new Region();
                HBox.setHgrow(rSpacer, Priority.ALWAYS);


                //cRow.getChildren().addAll(textSide, spacer, delComm);
                HBox rActions = new HBox(5);
                rActions.setAlignment(Pos.CENTER_RIGHT);

                Button delReply = new Button("Supprimer");
                delReply.setStyle("-fx-background-color: #fee; -fx-text-fill: #c00; -fx-font-size: 10;");
                delReply.setOnAction(e -> {
                    try {
                        serviceCommentaire.DeleteComment(r.getIdCommentaire());
                        showPostDetails(post); // Refresh this view
                    } catch (SQLException ex) { ex.printStackTrace(); }
                });
                rActions.getChildren().add(delReply);

                // UPDATED: Added rRoleBadge to the reply header
                rHeader.getChildren().addAll(rUser, rRoleBadge, rSpacer, rActions);
                Label rContent = new Label(r.getContenu());
                rContent.setWrapText(true);

                rBox.getChildren().addAll(rHeader, rContent);
                commentSection.getChildren().add(rBox);
            }

        }

        detailRoot.getChildren().addAll(backBtn, title, content, new Separator(), commentSection);
        postsContainer.getChildren().add(detailRoot);
    }

    private boolean confirm(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        return alert.getResult() == ButtonType.YES;
    }

     // Ensure this is initialized

    private void handleDeletePost(PostView post) {
        // 1. Confirmation Dialog (Best practice for deletions)
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la publication ?");
        alert.setContentText("Voulez-vous vraiment supprimer le post de " + post.getUsername() + " ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                // 2. Call the service method you wrote
                servicePost.DeletePost(post.getIdPost());

                // 3. Refresh the UI to show the post is gone
                loadPosts();

                // Optional: Show success message
                System.out.println("Post deleted successfully from UI.");
            } catch (SQLException e) {
                e.printStackTrace();
                // Show error alert if database fails
            }
        }
    }
    @FXML
    private void handleLogout(ActionEvent event) { // Add ActionEvent as a parameter
        SessionManager.setCurrentUser(null);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vita/devora/Login.fxml"));
            Parent root = loader.load();

            // Use the event source to get the stage safely
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new javafx.scene.Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}