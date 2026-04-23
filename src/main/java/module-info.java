module com.example.vita {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.vita to javafx.fxml;
    exports com.example.vita;
}