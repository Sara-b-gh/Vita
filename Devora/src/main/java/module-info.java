module com.vita.devora {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.vita.devora to javafx.fxml;
    exports com.vita.devora;
}