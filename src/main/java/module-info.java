module com.example.vita {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.mail;
    requires okhttp3;
    requires org.json;

    opens com.example.vita to javafx.fxml;
    opens controles to javafx.fxml;
    opens entities to javafx.fxml;
    opens utils to javafx.fxml;

    exports com.example.vita;
    exports controles;
}