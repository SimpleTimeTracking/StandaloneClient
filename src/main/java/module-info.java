module org.stt {
    requires javafx.controls;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.fxml;

    requires java.logging;
    requires java.desktop;

    requires kotlin.stdlib;
    requires kotlin.stdlib.jdk7;
    requires javax.inject;
    requires dagger;
    requires java.compiler;
    requires mbassador;
    requires jsoniter;
    requires org.controlsfx.controls;
    requires org.fxmisc.richtext;
    requires org.antlr.antlr4.runtime;
    requires java.net.http;

    // exporting is needed for javafx to work
    opens org.stt;
    opens org.stt.gui;
    // export is needed so jsoniter works (using reflection)
    opens org.stt.config;
    // export is needed so mbassy works (using reflection)
    opens org.stt.event;
    opens org.stt.query;
    opens org.stt.gui.jfx;
}