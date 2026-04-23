module com.example.vita {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.vita to javafx.fxml;
    exports com.example.vita;

    opens entities;
    opens services;
    opens utils;
    opens tests;
}