package org.stt.gui.jfx;

import dagger.Module;
import dagger.Provides;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Module
public class JFXModule {
    private JFXModule() {
    }

    @Singleton
    @Provides
    static Stage stageProvider() {
        return new Stage();
    }

    @Provides
    @Named("glyph")
    static Font provideFont() {
        try (InputStream fontStream = JFXModule.class.getResourceAsStream("/fontawesome-webfont.ttf")) {
            return Font.loadFont(fontStream, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
