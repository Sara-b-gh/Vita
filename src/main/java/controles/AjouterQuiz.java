package controles;

import controles.Question;   // ✅ corrigé : était "entities.Question"
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class AjouterQuiz {

    @FXML private Button btnVider;

    @FXML private TableColumn<Question, Integer> colId;
    @FXML private TableColumn<Question, String>  colQuestion;
    @FXML private TableColumn<Question, String>  colA;
    @FXML private TableColumn<Question, String>  colB;
    @FXML private TableColumn<Question, String>  colC;
    @FXML private TableColumn<Question, String>  colD;
    @FXML private TableColumn<Question, String>  colBonne;

    @FXML private ComboBox<String>    comboBonneRep;
    @FXML private TableView<Question> tableQuestions;

    @FXML private TextField txtA;
    @FXML private TextField txtB;
    @FXML private TextField txtC;
    @FXML private TextField txtD;
    @FXML private TextField txtId;
    @FXML private TextField txtQuestion;
    @FXML private TextField txtRecherche;

    private ObservableList<Question> list = FXCollections.observableArrayList();
    Connection conn;

    @FXML
    public void initialize() {
        conn = MyDatabase.getInstance().getConnection();

        comboBonneRep.setItems(FXCollections.observableArrayList("A", "B", "C", "D"));

        colId.setCellValueFactory(data -> data.getValue().idProperty().asObject());
        colQuestion.setCellValueFactory(data -> data.getValue().questionProperty());
        colA.setCellValueFactory(data -> data.getValue().reponseAProperty());
        colB.setCellValueFactory(data -> data.getValue().reponseBProperty());
        colC.setCellValueFactory(data -> data.getValue().reponseCProperty());
        colD.setCellValueFactory(data -> data.getValue().reponseDProperty());
        colBonne.setCellValueFactory(data -> data.getValue().bonneReponseProperty());

        loadData();

        tableQuestions.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, q) -> {
                    if (q != null) {
                        txtId.setText(String.valueOf(q.getId()));
                        txtQuestion.setText(q.getQuestion());
                        txtA.setText(q.getReponseA());
                        txtB.setText(q.getReponseB());
                        txtC.setText(q.getReponseC());
                        txtD.setText(q.getReponseD());
                        comboBonneRep.setValue(q.getBonneReponse());
                    }
                });
    }

    // ================= LOAD =================
    public void loadData() {
        list.clear();
        try {
            String sql = "SELECT * FROM question";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(new Question(
                        rs.getInt("id"),
                        rs.getString("question"),
                        rs.getString("reponseA"),
                        rs.getString("reponseB"),
                        rs.getString("reponseC"),
                        rs.getString("reponseD"),
                        rs.getString("bonneReponse")
                ));
            }
            tableQuestions.setItems(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= ADD =================
    @FXML
    void AjouterQuiz(ActionEvent event) {
        if (!validerChamps()) return;
        try {
            String sql = "INSERT INTO question(question, reponseA, reponseB, reponseC, reponseD, bonneReponse) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, txtQuestion.getText().trim());
            ps.setString(2, txtA.getText().trim());
            ps.setString(3, txtB.getText().trim());
            ps.setString(4, txtC.getText().trim());
            ps.setString(5, txtD.getText().trim());
            ps.setString(6, comboBonneRep.getValue());
            ps.executeUpdate();
            showAlert("Succès", "Question ajoutée !");
            loadData();
            clearFields();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= UPDATE =================
    @FXML
    void ModifierQuiz(ActionEvent event) {
        if (txtId.getText().isEmpty()) {
            showAlert("Attention", "Sélectionne d'abord une ligne dans la table.");
            return;
        }
        if (!validerChamps()) return;
        try {
            String sql = "UPDATE question SET question=?, reponseA=?, reponseB=?, "
                    + "reponseC=?, reponseD=?, bonneReponse=? WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, txtQuestion.getText().trim());
            ps.setString(2, txtA.getText().trim());
            ps.setString(3, txtB.getText().trim());
            ps.setString(4, txtC.getText().trim());
            ps.setString(5, txtD.getText().trim());
            ps.setString(6, comboBonneRep.getValue());
            ps.setInt(7, Integer.parseInt(txtId.getText()));
            ps.executeUpdate();
            showAlert("Succès", "Question modifiée !");
            loadData();
            clearFields();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DELETE =================
    @FXML
    void SupprimerQuiz(ActionEvent event) {
        if (txtId.getText().isEmpty()) {
            showAlert("Attention", "Sélectionne d'abord une ligne dans la table.");
            return;
        }
        // Confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cette question ?");
        confirm.setContentText("Cette action est irréversible.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            String sql = "DELETE FROM question WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(txtId.getText()));
            ps.executeUpdate();
            showAlert("Succès", "Question supprimée !");
            loadData();
            clearFields();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= VIDER =================
    @FXML
    void Vider(ActionEvent event) {
        clearFields();
    }

    // ================= UTILS =================
    private boolean validerChamps() {
        if (txtQuestion.getText().trim().isEmpty()
                || txtA.getText().trim().isEmpty()
                || txtB.getText().trim().isEmpty()
                || txtC.getText().trim().isEmpty()
                || txtD.getText().trim().isEmpty()
                || comboBonneRep.getValue() == null) {
            showAlert("Champs manquants", "Veuillez remplir tous les champs.");
            return false;
        }
        return true;
    }

    private void clearFields() {
        txtId.clear();
        txtQuestion.clear();
        txtA.clear();
        txtB.clear();
        txtC.clear();
        txtD.clear();
        comboBonneRep.setValue(null);
        tableQuestions.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    @FXML
    private void viderAction(ActionEvent event) {
        txtId.clear();
        txtQuestion.clear();
        txtA.clear();
        txtB.clear();
        txtC.clear();
        txtD.clear();
        comboBonneRep.getSelectionModel().clearSelection();
    }
}