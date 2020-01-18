module org.nagoya.japsearch {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.swing;
    requires javafx.web;

    requires reactfx;
    requires livedirsfx;
    requires org.controlsfx.controls;
    requires com.jfoenix;
    requires de.jensd.fx.glyphs.fontawesome;

    requires selenium.api;
    requires selenium.chrome.driver;
    requires webdrivermanager;
    requires io.webfolder.cdp4j;

    requires org.jetbrains.annotations;
    requires java.desktop;
    requires vavr;
    requires cyclops;
    requires com.google.gson;
    requires io.vavr.gson;
    requires fx.gson;

    requires java.sql;
    requires com.github.benmanes.caffeine;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
    requires org.apache.commons.codec;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires xstream;
    requires org.jsoup;
    requires org.reactivestreams;
    // requires io.reactivex.rxjava3;
    requires reactor.core;
    //requires rxjavafx;
    requires jpro.webapi;
    requires javafx.media;

    requires org.nagoya.fx;
    requires org.nagaya.video;
    requires org.nagoya.commons;
    requires org.nagoya.io;

    exports org.nagoya;
    exports org.nagoya.view;
    exports org.nagoya.system;
    exports org.nagoya.system.event;
    exports org.nagoya.system.database;
    exports org.nagoya.system.dialog;
    exports org.nagoya.model;
    exports org.nagoya.model.dataitem;
    exports org.nagoya.preferences;
    exports org.nagoya.view.dialog;
    exports org.nagoya.controller;
    exports org.nagoya.controller.siteparsingprofile;
    exports org.nagoya.controller.siteparsingprofile.specific;
    exports org.nagoya.controller.languagetranslation;
    exports org.nagoya.view.skin;
    exports org.nagoya.view.skin.effects;
    exports net.raumzeitfalle.fx;

    exports org.nagoya.controls;

    opens org.nagoya.controls;

    opens net.raumzeitfalle.fx.filechooser to javafx.fxml;
    opens org.nagoya to javafx.fxml;
    opens org.nagoya.view to javafx.fxml;
    opens org.nagoya.view.dialog to javafx.fxml;
    opens org.nagoya.view.customcell to javafx.fxml;
    opens org.nagoya.controller to javafx.fxml;

    opens org.nagoya.model.dataitem;
    opens org.nagoya.preferences;

    opens org.nagoya.view.editor to javafx.base;

    opens org.nagoya.model.xmlserialization to xstream;

}