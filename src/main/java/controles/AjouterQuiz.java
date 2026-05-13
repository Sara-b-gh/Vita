package controles;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AjouterQuiz {

    @FXML private Button btnVider;
    @FXML private ComboBox<String> comboBonneRep;
    @FXML private TextField txtA;
    @FXML private TextField txtB;
    @FXML private TextField txtC;
    @FXML private TextField txtD;
    @FXML private TextField txtId;
    @FXML private TextField txtQuestion;

    Connection conn;

    @FXML
    public void initialize() {
        conn = MyDatabase.getInstance().getConnection();
        comboBonneRep.setItems(FXCollections.observableArrayList("A", "B", "C", "D"));
    }

    // ================= ADD =================
    @FXML
    void AjouterQuiz(ActionEvent event) {
        if (!validerChamps()) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO question(question,reponseA,reponseB,reponseC,reponseD,bonneReponse) VALUES(?,?,?,?,?,?)");
            ps.setString(1, txtQuestion.getText().trim());
            ps.setString(2, txtA.getText().trim());
            ps.setString(3, txtB.getText().trim());
            ps.setString(4, txtC.getText().trim());
            ps.setString(5, txtD.getText().trim());
            ps.setString(6, comboBonneRep.getValue());
            ps.executeUpdate();
            showAlert("Succès", "Question ajoutée !");
            clearFields();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================= UPDATE =================
    @FXML
    void ModifierQuiz(ActionEvent event) {
        if (txtId.getText().isEmpty()) {
            showAlert("Attention", "Sélectionne d'abord une ligne dans la liste.");
            return;
        }
        if (!validerChamps()) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE question SET question=?,reponseA=?,reponseB=?,reponseC=?,reponseD=?,bonneReponse=? WHERE id=?");
            ps.setString(1, txtQuestion.getText().trim());
            ps.setString(2, txtA.getText().trim());
            ps.setString(3, txtB.getText().trim());
            ps.setString(4, txtC.getText().trim());
            ps.setString(5, txtD.getText().trim());
            ps.setString(6, comboBonneRep.getValue());
            ps.setInt(7, Integer.parseInt(txtId.getText()));
            ps.executeUpdate();
            showAlert("Succès", "Question modifiée !");
            clearFields();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================= DELETE =================
    @FXML
    void SupprimerQuiz(ActionEvent event) {
        if (txtId.getText().isEmpty()) {
            showAlert("Attention", "Sélectionne d'abord une ligne dans la liste.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer cette question ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            int id = Integer.parseInt(txtId.getText());

            PreparedStatement ps1 = conn.prepareStatement(
                    "DELETE FROM reponses_utilisateur WHERE question_id = ?");
            ps1.setInt(1, id);
            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement(
                    "DELETE FROM question WHERE id = ?");
            ps2.setInt(1, id);
            ps2.executeUpdate();

            showAlert("Succès", "Question supprimée !");
            clearFields();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================= VIDER =================
    @FXML
    private void viderAction(ActionEvent event) {
        clearFields();
    }

    // ================= VALIDATION =================
    private boolean validerChamps() {
        String question = txtQuestion.getText().trim();
        String repA = txtA.getText().trim();
        String repB = txtB.getText().trim();
        String repC = txtC.getText().trim();
        String repD = txtD.getText().trim();

        // 1. Champs vides
        if (question.isEmpty() || repA.isEmpty() || repB.isEmpty()
                || repC.isEmpty() || repD.isEmpty()
                || comboBonneRep.getValue() == null) {
            showAlert("Champs manquants", "⚠ Veuillez remplir tous les champs.");
            return false;
        }

        // 2. Question trop courte
        if (question.length() < 10) {
            showAlert("Question invalide",
                    "❌ La question est trop courte (minimum 10 caractères).");
            return false;
        }

        // 3. Question doit se terminer par '?'
        if (!question.endsWith("?")) {
            showAlert("Question invalide",
                    "❌ La question doit se terminer par '?'");
            return false;
        }

        // 4. Réponses toutes différentes
        String[] reps = {repA, repB, repC, repD};
        String[] noms = {"A", "B", "C", "D"};
        for (int i = 0; i < reps.length; i++) {
            for (int j = i + 1; j < reps.length; j++) {
                if (reps[i].equalsIgnoreCase(reps[j])) {
                    showAlert("Réponses invalides",
                            "❌ Les réponses " + noms[i] + " et " + noms[j] +
                                    " sont identiques !");
                    return false;
                }
            }
        }

        // 5. Mots médicaux + moral/bien-être
        String[] motsMedicaux = {
                // ── Médical ─────────────────────────────────────────
                "médical", "santé", "corps", "maladie", "symptôme", "traitement",
                "organe", "sang", "coeur", "poumon", "cerveau", "muscle", "os",
                "virus", "bactérie", "vaccin", "médicament", "dose", "patient",
                "docteur", "médecin", "hôpital", "chirurgie", "diagnostic",
                "vitamine", "protéine", "calorie", "eau", "oxygène", "tension",
                "diabète", "cancer", "infection", "fièvre", "douleur", "blessure",
                "artère", "veine", "neurone", "hormone", "enzyme", "cellule",
                "litre", "gramme", "température", "pression", "alimentation",
                "recommandé", "normal", "humain", "adulte", "enfant", "âge",
                "foie", "rein", "rate", "pancréas", "estomac", "intestin",
                "peau", "oeil", "oreille", "nez", "bouche", "dent",
                "anémie", "allergie", "hypertension", "cholestérol", "obésité",
                // ── Moral / Bien-être mental ─────────────────────────
                "moral", "stress", "anxiété", "dépression", "bonheur", "émotion",
                "mental", "psychologique", "humeur", "sommeil", "méditation",
                "relaxation", "yoga", "thérapie", "psychiatre", "psychologue",
                "sérotonine", "bien-être", "tristesse", "trouble", "peur",
                "jogging", "sport", "exercice", "tryptophane", "cortisol",
                "adrénaline", "burnout", "fatigue", "motivation", "confiance",
                "social", "isolement", "cognitif", "comportement",
                "respiration", "marche", "activité", "conscience"
        };

        boolean contientMot = false;
        String questionLower = question.toLowerCase();
        for (String mot : motsMedicaux) {
            if (questionLower.contains(mot.toLowerCase())) {
                contientMot = true;
                break;
            }
        }

        if (!contientMot) {
            showAlert("Question invalide",
                    "❌ La question doit être liée à la santé, au médical ou au bien-être mental.\n" +
                            "Exemple : 'Quelle hormone est appelée hormone du bonheur ?'");
            return false;
        }

        return true;
    }

    // ================= UTILS =================
    private void clearFields() {
        txtId.clear();
        txtQuestion.clear();
        txtA.clear(); txtB.clear();
        txtC.clear(); txtD.clear();
        comboBonneRep.setValue(null);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void remplirChamps(int id, String question,
                              String a, String b, String c, String d,
                              String bonne) {
        txtId.setText(String.valueOf(id));
        txtQuestion.setText(question);
        txtA.setText(a);
        txtB.setText(b);
        txtC.setText(c);
        txtD.setText(d);
        comboBonneRep.setValue(bonne);
    }
}
