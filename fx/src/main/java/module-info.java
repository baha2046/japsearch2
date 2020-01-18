module org.nagoya.fx {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.base;
    requires java.logging;
    requires com.jfoenix;

    exports org.nagoya.fx.scene;
    opens org.nagoya.fx.scene;
}