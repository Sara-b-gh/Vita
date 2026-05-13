package controles;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("⚠ Veuillez remplir tous les champs.");
            return;
        }

        try {
            Connection conn = MyDatabase.getInstance().getConnection();

            // ✅ Ajouter email et username dans le SELECT
            String sql = "SELECT id, role, email, username FROM users " +
                    "WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role  = rs.getString("role");
                int uid      = rs.getInt("id");
                String email = rs.getString("email");
                String uname = rs.getString("username");

                if (role.equals("admin")) {
                    ouvrirPage("/QuizView.fxml");
                } else {
                    ouvrirPasserQuiz(uid, email, uname);
                }
            } else {
                errorLabel.setText("❌ Identifiants incorrects.");
            }

        } catch (Exception e) {
            errorLabel.setText("⚠ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ouvrirPage(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            errorLabel.setText("⚠ Erreur chargement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Passer email + username au PasserQuizController
    private void ouvrirPasserQuiz(int userId, String email, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/PasserQuiz.fxml"));
            Parent root = loader.load();
            PasserQuizController ctrl = loader.getController();
            ctrl.setUserInfo(userId, email, username);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            errorLabel.setText("⚠ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
