package com.vita.devora.Controlleurs;

import javafx.event.ActionEvent;

public class AdminPatientController {
    public void toDash(ActionEvent actionEvent) {
        switchPage(actionEvent,"/com/vita/devora/AdminDashbord.fxml");
    }

    public void DoctorDash(ActionEvent actionEvent) {
        switchPage(actionEvent,"/com/vita/devora/AdminDocteur.fxml");
    }

    private void switchPage(ActionEvent event, String fxmlPath) {
        try {
            // 1. Check if resource exists before loading to avoid generic IOExceptions
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("❌ FXML File not found at: " + fxmlPath);
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();

            // 2. Get the Stage safely
            javafx.scene.Node sourceNode = (javafx.scene.Node) event.getSource();
            javafx.stage.Stage stage = (javafx.stage.Stage) sourceNode.getScene().getWindow();

            // 3. Set the new root
            stage.getScene().setRoot(root);

            // Optional: If you want to ensure the window adjusts to the new size
            // stage.sizeToScene();

        } catch (java.io.IOException e) {
            System.err.println("❌ Critical error loading: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void handleAddPatient(ActionEvent actionEvent) {
    }
}
