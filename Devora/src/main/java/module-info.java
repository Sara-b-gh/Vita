module com.vita.devora {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.management;
    requires java.mail;
    requires java.prefs;
    requires jdk.jsobject;
    requires jdk.httpserver;


    opens com.vita.devora to javafx.fxml;
    opens com.vita.devora.Controlleurs to javafx.fxml;
    opens com.vita.devora.Entities to javafx.base, javafx.fxml;
    exports com.vita.devora;
}
