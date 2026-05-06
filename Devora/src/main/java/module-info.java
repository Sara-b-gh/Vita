module com.vita.devora {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.management;
    requires java.mail;
    requires java.prefs;


    opens com.vita.devora to javafx.fxml;
    opens com.vita.devora.Controlleurs to javafx.fxml;
    opens com.vita.devora.Entities to javafx.base, javafx.fxml;
    exports com.vita.devora;
}