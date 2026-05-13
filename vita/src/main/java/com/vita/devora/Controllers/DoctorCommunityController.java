package com.vita.devora.Controllers;

import com.vita.devora.Entities.Commentaire;
import com.vita.devora.Entities.Post;
import com.vita.devora.Entities.PostView;
import com.vita.devora.Entities.user;
import com.vita.devora.Services.ServicePost;
import com.vita.devora.Services.ServiceCommentaire;
import com.vita.devora.Utils.sessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DoctorCommunityController implements Initializable {

    @FXML private VBox postsContainer;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> sortFilter;
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox mainLayout;

    private final ServicePost servicePost = new ServicePost();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        user loggedIn = sessionManager.getCurrentUser();
        if (loggedIn != null) {
            System.out.println("Welcome, " + loggedIn.getNom());
        }
        setupFilters();
        loadPosts();
    }

    private void setupFilters() {
        // Category Filter
        typeFilter.getItems().addAll("Tous", "QUESTION", "Advice", "OPPINION", "Story");
        typeFilter.setValue("Tous");

        // Sort Filter
        sortFilter.getItems().addAll("Plus récent", "Plus ancien");
        sortFilter.setValue("Plus récent");

        // Set actions to trigger unified filtering logic
        typeFilter.setOnAction(e -> applyFilters());
        sortFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        postsContainer.getChildren().clear();

        // 1. Always start with the base sorted list from service
        List<PostView> posts = servicePost.getAllPostsSortedByDateDesc();

        // 2. Apply Category Filter
        String selectedCategory = typeFilter.getValue();
        if (!selectedCategory.equals("Tous")) {
            posts = posts.stream()
                    .filter(p -> p.getCategory().equalsIgnoreCase(selectedCategory))
                    .collect(Collectors.toList());
        }

        // 3. Apply Chronological Sort
        // Since the service already gives Descending (Recent),
        // we only reverse if "Plus ancien" is selected.
        if ("Plus ancien".equals(sortFilter.getValue())) {
            Collections.reverse(posts);
        }

        // 4. Render the cards
        for (PostView post : posts) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }

    private void loadPosts() {
        applyFilters();
//        postsContainer.getChildren().clear();
//        List<PostView> posts = servicePost.getAllPostsSortedByDateDesc();
//        for (PostView post : posts) {
//            // 3. Add each card to the container
//            VBox card = createPostCard(post);
//            postsContainer.getChildren().add(card);
//        }
    }
    private VBox createPostCard(PostView post) {


        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPadding(new Insets(12, 16, 12, 16));

        HBox header = new HBox();
        Label title = new Label(post.getContenu().substring(0, Math.min(post.getContenu().length(), 40)) + "...");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(post.getCategory().toLowerCase());
        badge.setStyle(getBadgeStyle(post.getCategory()));

        header.getChildren().addAll(title, spacer, badge);

        Label content = new Label(post.getContenu());
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: #666;");

        HBox authorBox = new HBox(10);
        Label authorName = new Label(post.getUsername());
        authorName.setStyle("-fx-font-weight: bold;");

        System.out.println("Post by: " + post.getUsername() + " | Role found: " + post.getUserRole());
        String userRole = (post.getUserRole() != null) ? post.getUserRole().toUpperCase() : "USER";
        Label roleBadge = new Label(userRole);
        roleBadge.setStyle(getRoleBadgeStyle(userRole));
        authorBox.getChildren().addAll(authorName, roleBadge);

        HBox footer = new HBox(15);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox commentBox = new HBox(5); // 5px spacing between icon and text
        commentBox.setAlignment(Pos.CENTER_LEFT);
        // 1. Create the ImageView for the chat icon
        ImageView chatIcon = new ImageView(new Image(getClass().getResourceAsStream("/com/vita/devora/imgs/chat.png")));
        chatIcon.setFitHeight(15.0);
        chatIcon.setFitWidth(17.0);
        chatIcon.setPickOnBounds(true);
        chatIcon.setPreserveRatio(true);
        Label nbCommentsLabel = new Label(String.valueOf(post.getNbCommentaire()));
        nbCommentsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        commentBox.getChildren().addAll(chatIcon, nbCommentsLabel);
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        HBox ownerActions = new HBox(10);
        ownerActions.setAlignment(Pos.CENTER_RIGHT);
        user currentUser = sessionManager.getCurrentUser();
        System.out.println("DEBUG: Post Owner ID = " + post.getIdUser());
        System.out.println("DEBUG: Logged-in user ID = " + (currentUser != null ? currentUser.getId() : "NULL"));
        if (currentUser != null && post.getIdUser() == currentUser.getId()) {
            // Only show buttons if the IDs match
            Button btnEdit = new Button("Modifier");
            btnEdit.setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-background-radius: 5; -fx-font-size: 11; -fx-cursor: hand;");
            btnEdit.setOnAction(e -> handleEditPost(post));

            Button btnDelete = new Button("Supprimer");
            btnDelete.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-background-radius: 5; -fx-font-size: 11; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> handleDeletePost(post));

            ownerActions.getChildren().addAll(btnEdit, btnDelete);
        }

        Button btnReply = new Button("Répondre");
        btnReply.setStyle("-fx-background-color: #8a0037; -fx-text-fill: white; -fx-background-radius: 6;");
        btnReply.setOnAction(e -> handleReply(post));

        Button btnView = new Button("Voir");
        btnView.setStyle("-fx-background-color: #e8f4fd;");
        btnView.setOnAction(e -> openPostDetails(post));

        footer.getChildren().addAll( commentBox, footerSpacer,ownerActions, btnView);

        card.getChildren().addAll(header, content, authorBox, footer);
        return card;
    }

    private String getRoleBadgeStyle(String role) {
        if (role == null)
            return "-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; -fx-background-radius: 8; -fx-padding: 2 6;";

        switch (role.toUpperCase()) {
            case "DOCTOR":
                return "-fx-background-color: #ffe0e9; -fx-text-fill: #8a0037; -fx-background-radius: 8; -fx-padding: 2 6; -fx-font-weight: bold;";
            case "ADMIN":
                return "-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-background-radius: 8; -fx-padding: 2 6; -fx-font-weight: bold;";
            case "USER":
            default:
                return "-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; -fx-background-radius: 8; -fx-padding: 2 6;";
        }
    }
    private String getBadgeStyle(String category) {
        switch (category.toUpperCase()) {
            case "QUESTION": return "-fx-background-color: #d0eaff; -fx-text-fill: #1565c0; -fx-background-radius: 10; -fx-padding: 2 8;";
            case "CONSEIL": return "-fx-background-color: #d4f7e0; -fx-text-fill: #1b6b3a; -fx-background-radius: 10; -fx-padding: 2 8;";
            default: return "-fx-background-color: #eee; -fx-text-fill: #555; -fx-background-radius: 10; -fx-padding: 2 8;";
        }
    }

    private void filterPosts(String category) {
        postsContainer.getChildren().clear();
        List<PostView> posts = servicePost.filterByCategory(Post.Category.valueOf(category));
        for (PostView post : posts) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }


    private void handleReply(PostView post) {
        // Implementation for opening a comment dialog or navigating to detail view
        System.out.println("Replying to post ID: " + post.getIdPost());
    }

    @FXML
    private void handleNewPost() {
        // 1. Create the Pop-up Window (Stage)
        javafx.stage.Stage popupStage = new javafx.stage.Stage();
        popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popupStage.setTitle("Nouvelle Publication");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");

        Label lblTitle = new Label("Partager avec la communauté");
        lblTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #8a0037;");

        // 2. Category Selection
        ComboBox<Post.Category> catCombo = new ComboBox<>();
        catCombo.getItems().addAll(Post.Category.values());
        catCombo.setPromptText("Choisir une catégorie");
        catCombo.setMaxWidth(Double.MAX_VALUE);

        // 3. Content Area
        TextArea txtContent = new TextArea();
        txtContent.setPromptText("Que voulez-vous dire ?");
        txtContent.setWrapText(true);
        txtContent.setPrefHeight(150);

        // 4. Save Button
        Button btnSave = new Button("Publier");
        btnSave.setStyle("-fx-background-color: #8a0037; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnSave.setMaxWidth(Double.MAX_VALUE);

        btnSave.setOnAction(e -> {
            if (catCombo.getValue() != null && !txtContent.getText().isEmpty()) {
                saveNewPost(catCombo.getValue(), txtContent.getText());
                popupStage.close();
                applyFilters(); // Refresh the list immediately
            }
        });

        layout.getChildren().addAll(lblTitle, catCombo, txtContent, btnSave);

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 400, 350);
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private void openPostDetails(PostView post) {
        VBox detailsContainer = new VBox(20);
        detailsContainer.setStyle("-fx-padding: 30; -fx-background-color: #F8F9FA;");

        // --- Navigation ---
        Button btnBack = new Button("← Retour à la liste");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #8a0037; -fx-font-weight: bold; -fx-cursor: hand;");
        btnBack.setOnAction(e -> {
            mainScrollPane.setContent(mainLayout);
            applyFilters();
        });

        // --- Post Header ---
        VBox postHeader = new VBox(10);
        postHeader.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: #ddd; -fx-border-radius: 12;");
        Label title = new Label(post.getContenu());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        Label meta = new Label("Posté par " + post.getUsername() + " • " + post.getCategory());
        meta.setStyle("-fx-text-fill: #888;");
        postHeader.getChildren().addAll(title, meta);

        // --- Comments List ---
        VBox commentsList = new VBox(15);
        Label lblTitle = new Label("Commentaires (" + post.getNbCommentaire() + ")");
        lblTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        List<Commentaire> comments = serviceCommentaire.getByPost(post.getIdPost());
        user currentUser = sessionManager.getCurrentUser();

        for (Commentaire c : comments) {
            // --- Parent Comment UI ---
            VBox cBox = new VBox(8);
            cBox.setStyle("-fx-background-color: #ffffff; -fx-padding: 15; -fx-background-radius: 10; -fx-border-color: #eee;");

            HBox commentHeader = new HBox(10);
            commentHeader.setAlignment(Pos.CENTER_LEFT);

            // CHANGED: Display Name instead of ID
            Label cUser = new Label(c.getUsername() != null ? c.getUsername() : "Utilisateur " + c.getIdUser());
            cUser.setStyle("-fx-font-weight: bold; -fx-text-fill: #8a0037;");

            // ADDED: Display Role Badge for main comments
            String cRoleStr = (c.getUserRole() != null) ? c.getUserRole().toUpperCase() : "USER";
            Label cRoleBadge = new Label(cRoleStr);
            cRoleBadge.setStyle(getRoleBadgeStyle(cRoleStr));
            cRoleBadge.setScaleX(0.9); cRoleBadge.setScaleY(0.9); // Slightly smaller for comments

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox commentActions = new HBox(5);
            commentActions.setAlignment(Pos.CENTER_RIGHT);

            // Parent Actions
            Button btnReplyComm = new Button("Répondre");
            btnReplyComm.setStyle("-fx-background-color: #e8f4fd; -fx-text-fill: #1565c0; -fx-font-size: 10; -fx-cursor: hand;");
            btnReplyComm.setOnAction(e -> handleReplyToComment(c, post));
            commentActions.getChildren().add(btnReplyComm);

            if (currentUser != null && c.getIdUser() == currentUser.getId()) {
                Button btnEditComm = new Button("Modifier");
                btnEditComm.setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-font-size: 10;");
                btnEditComm.setOnAction(e -> handleEditComment(c, post));

                Button btnDeleteComm = new Button("Supprimer");
                btnDeleteComm.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-font-size: 10;");
                btnDeleteComm.setOnAction(e -> handleDeleteComment(c.getIdCommentaire(), post));

                commentActions.getChildren().addAll(btnEditComm, btnDeleteComm);
            }

            // UPDATED: Added cRoleBadge to the header
            commentHeader.getChildren().addAll(cUser, cRoleBadge, spacer, commentActions);
            Label cContent = new Label(c.getContenu());
            cContent.setWrapText(true);
            cBox.getChildren().addAll(commentHeader, cContent);
            commentsList.getChildren().add(cBox);

            // --- Threaded Replies UI ---
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
                rRoleBadge.setStyle(getRoleBadgeStyle(rRoleStr));
                rRoleBadge.setScaleX(0.8); rRoleBadge.setScaleY(0.8); // Smaller badge for nested replies

                Region rSpacer = new Region();
                HBox.setHgrow(rSpacer, Priority.ALWAYS);

                HBox rActions = new HBox(5);
                rActions.setAlignment(Pos.CENTER_RIGHT);

                if (currentUser != null && r.getIdUser() == currentUser.getId()) {
                    Button btnEditReply = new Button("Modifier");
                    btnEditReply.setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-font-size: 9; -fx-cursor: hand;");
                    btnEditReply.setOnAction(e -> handleEditComment(r, post));

                    Button btnDeleteReply = new Button("Supprimer");
                    btnDeleteReply.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-font-size: 9; -fx-cursor: hand;");
                    btnDeleteReply.setOnAction(e -> handleDeleteComment(r.getIdCommentaire(), post));

                    rActions.getChildren().addAll(btnEditReply, btnDeleteReply);
                }

                // UPDATED: Added rRoleBadge to the reply header
                rHeader.getChildren().addAll(rUser, rRoleBadge, rSpacer, rActions);
                Label rContent = new Label(r.getContenu());
                rContent.setWrapText(true);

                rBox.getChildren().addAll(rHeader, rContent);
                commentsList.getChildren().add(rBox);
            }
        }

        // --- Main Post Reply Area ---
        VBox replyBox = new VBox(10);
        TextArea txtReply = new TextArea();
        txtReply.setPromptText("Écrivez un commentaire...");
        txtReply.setPrefHeight(80);
        Button btnSubmit = new Button("Commenter");
        btnSubmit.setStyle("-fx-background-color: #8a0037; -fx-text-fill: white; -fx-background-radius: 8;");
        btnSubmit.setOnAction(e -> {
            if (!txtReply.getText().isEmpty()) {
                handleSaveComment(post.getIdPost(), txtReply.getText());
                openPostDetails(post);
            }
        });
        replyBox.getChildren().addAll(txtReply, btnSubmit);

        detailsContainer.getChildren().addAll(btnBack, postHeader, lblTitle, commentsList, new Separator(), replyBox);
        mainScrollPane.setContent(detailsContainer);
    }

    private void handleSaveComment(int postId, String content) {
        try {
            user currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) return;
            Commentaire newComment = new Commentaire();
            newComment.setIdPost(postId);
            newComment.setIdUser(currentUser.getId());
            newComment.setContenu(content);
            newComment.setStatut(Commentaire.Statut.VISIBLE);
            newComment.setDateCreation(LocalDateTime.now());
            newComment.setDateModification(LocalDateTime.now());

            serviceCommentaire.AddComment(newComment);
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveNewPost(Post.Category category, String content) {
        try {
            user sessionUser = sessionManager.getCurrentUser();
            if (sessionUser != null){
                Post p = new Post();
                // For your demo, use a static user ID (ensure ID 61 exists in your 'users' table)
                p.setIdUser(sessionUser.getId());
                p.setCategory(category);
                p.setContenu(content);
                p.setStatut(Post.Statut.ACTIF);
                p.setDateCreation(LocalDateTime.now());
                p.setDateModification(LocalDateTime.now());
                p.setNbCommentaire(0);

                servicePost.AddPost(p);
                applyFilters();
            }

        } catch (Exception e) {
            System.err.println("Error saving post: " + e.getMessage());
        }

    }

    private void handleDeletePost(PostView postView) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la publication ?");
        alert.setContentText("Cette action est irréversible.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                servicePost.DeletePost(postView.getIdPost());
                applyFilters(); // Refresh the list immediately
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleEditPost(PostView postView) {
        javafx.stage.Stage popupStage = new javafx.stage.Stage();
        popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        TextArea txtContent = new TextArea(postView.getContenu());
        txtContent.setWrapText(true);

        Button btnUpdate = new Button("Enregistrer");
        btnUpdate.setStyle("-fx-background-color: #8a0037; -fx-text-fill: white;");

        btnUpdate.setOnAction(e -> {
            try {
                // Re-fetch the full Post entity to update it
                Post p = servicePost.GetPostById(postView.getIdPost());
                p.setContenu(txtContent.getText());
                p.setDateModification(LocalDateTime.now());

                servicePost.UpdatePost(p);
                popupStage.close();
                applyFilters();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        layout.getChildren().addAll(new Label("Modifier votre publication"), txtContent, btnUpdate);
        popupStage.setScene(new javafx.scene.Scene(layout, 400, 300));
        popupStage.showAndWait();
    }

    private void handleDeleteComment(int commentId, PostView currentPost) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le commentaire");
        alert.setContentText("Voulez-vous vraiment supprimer ce commentaire ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                serviceCommentaire.DeleteComment(commentId);
                // Refresh the details view to show it's gone
                openPostDetails(currentPost);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleEditComment(Commentaire comment, PostView currentPost) {
        javafx.stage.Stage popupStage = new javafx.stage.Stage();
        popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        TextArea txtComment = new TextArea(comment.getContenu());
        txtComment.setWrapText(true);
        txtComment.setPrefHeight(100);

        Button btnSave = new Button("Modifier");
        btnSave.setStyle("-fx-background-color: #8a0037; -fx-text-fill: white;");

        btnSave.setOnAction(e -> {
            try {
                comment.setContenu(txtComment.getText());
                serviceCommentaire.Updatecomment(comment);
                popupStage.close();
                openPostDetails(currentPost); // Refresh view
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        layout.getChildren().addAll(new Label("Modifier votre commentaire"), txtComment, btnSave);
        popupStage.setScene(new javafx.scene.Scene(layout, 350, 200));
        popupStage.showAndWait();
    }

    private void handleReplyToComment(Commentaire parentComment, PostView currentPost) {
        javafx.stage.Stage popupStage = new javafx.stage.Stage();
        popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popupStage.setTitle("Répondre au commentaire");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: white;");

        Label lblTarget = new Label("Réponse à: " + parentComment.getContenu().substring(0, Math.min(15, parentComment.getContenu().length())) + "...");
        lblTarget.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");

        TextArea txtReply = new TextArea();
        txtReply.setPromptText("Écrivez votre réponse...");
        txtReply.setWrapText(true);
        txtReply.setPrefHeight(100);

        Button btnSend = new Button("Répondre");
        btnSend.setStyle("-fx-background-color: #8a0037; -fx-text-fill: white;");
        btnSend.setMaxWidth(Double.MAX_VALUE);

        btnSend.setOnAction(e -> {
            if (!txtReply.getText().isEmpty()) {
                try {
                    user currentUser = sessionManager.getCurrentUser();
                    if (currentUser == null) return;

                    Commentaire reply = new Commentaire();
                    reply.setIdPost(parentComment.getIdPost());
                    reply.setIdUser(currentUser.getId());
                    reply.setContenu(txtReply.getText());
                    reply.setParentId(parentComment.getIdCommentaire()); // 👈 The Parent ID link
                    reply.setStatut(Commentaire.Statut.VISIBLE);
                    reply.setDateCreation(LocalDateTime.now());
                    reply.setDateModification(LocalDateTime.now());

                    serviceCommentaire.AddComment(reply);
                    popupStage.close();
                    openPostDetails(currentPost); // Refresh view
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        layout.getChildren().addAll(lblTarget, txtReply, btnSend);
        popupStage.setScene(new javafx.scene.Scene(layout, 350, 220));
        popupStage.showAndWait();
    }


    @FXML
    private void handleLogout() {
        // 1. Clear the session
        sessionManager.setCurrentUser(null); // Assuming your sessionManager has this setter

        try {
            // 2. Load the Login Scene (Update the path to your actual login FXML)
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/vita/devora/Login.fxml"));
            javafx.scene.Parent root = loader.load();

            // 3. Get the current stage from the logout button
            javafx.stage.Stage stage = (javafx.stage.Stage) mainScrollPane.getScene().getWindow();

            // 4. Set the scene and show
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

            System.out.println("user logged out successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}