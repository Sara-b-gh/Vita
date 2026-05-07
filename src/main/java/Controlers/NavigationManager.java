package Controlers;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;

public class NavigationManager {

    private static NavigationManager instance;
    private BorderPane mainContainer; // le BorderPane racine de la fenêtre principale

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) instance = new NavigationManager();
        return instance;
    }

    /** Appelé une seule fois au démarrage depuis votre Main/App */
    public void setMainContainer(BorderPane container) {
        this.mainContainer = container;
    }

    /** Charge un FXML et remplace le CENTER du conteneur principal */
    public <T> T navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationManager.class.getResource(fxmlPath));
            Node view = loader.load();
            mainContainer.setCenter(view);
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}