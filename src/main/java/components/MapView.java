package components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MapView extends VBox {

    public MapView(String titre, String adresse) {
        this.setStyle("-fx-background-color: #fdf5f7; -fx-border-color: #ead8de; -fx-border-radius: 10;");
        this.setPadding(new Insets(20));
        this.setSpacing(15);
        this.setAlignment(Pos.CENTER);
        this.setPrefSize(450, 280);

        Label iconLabel = new Label("🗺️");
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label titreLabel = new Label(titre);
        titreLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #5A1730;");
        titreLabel.setWrapText(true);
        titreLabel.setAlignment(Pos.CENTER);

        Label adresseLabel = new Label(adresse);
        adresseLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #A36277;");
        adresseLabel.setWrapText(true);
        adresseLabel.setAlignment(Pos.CENTER);

        Label instructionLabel = new Label("Cliquez sur 'Ouvrir dans Google Maps' pour voir l'itinéraire");
        instructionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #B78A98;");

        this.getChildren().addAll(iconLabel, titreLabel, adresseLabel, instructionLabel);
    }
}