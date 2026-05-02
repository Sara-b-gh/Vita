module com.example.vita {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.vita to javafx.fxml;
    opens controles to javafx.fxml;        // ✅ ajouter cette ligne
    opens entities to javafx.fxml;
    opens utils to javafx.fxml;

    exports com.example.vita;
    exports controles;
}