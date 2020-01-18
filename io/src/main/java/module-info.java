module org.nagoya.io {
    requires vavr;
    requires org.jetbrains.annotations;
    requires transitive javafx.base;
    requires org.jsoup;
    requires reactor.core;

    requires webdrivermanager;
    requires selenium.chrome.driver;
    requires io.webfolder.cdp4j;
    requires selenium.api;
    requires transitive commons.vfs2;

    requires org.nagoya.commons;
    requires com.google.gson;
    requires fx.gson;
    requires io.vavr.gson;

    exports org.nagoya.io;
    opens org.nagoya.io;
}