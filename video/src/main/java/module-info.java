module org.nagaya.video {
    requires vlcj;
    requires transitive javafx.graphics;
    requires transitive com.jfoenix;
    requires org.jetbrains.annotations;
    requires javafx.controls;

    requires org.nagoya.commons;

    exports org.nagoya.video.player;

    opens org.nagoya.video.player;
}