module com.example.vita {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires okhttp3;
    requires com.google.gson;

    opens com.example.vita.Controlers to javafx.fxml;
    opens com.example.vita.Entites to javafx.base;
    opens com.example.vita.main       to javafx.graphics;
    opens com.example.vita.services   to com.google.gson;

    exports com.example.vita.main;
}