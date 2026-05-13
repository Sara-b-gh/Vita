module com.vita.devora.vita {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.vita.devora to javafx.fxml;
    opens com.vita.devora.Controllers to javafx.fxml;
    exports com.vita.devora;
}