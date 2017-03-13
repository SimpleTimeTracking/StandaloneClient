package org.stt.gui.jfx;

import dagger.Module;
import dagger.Provides;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.stt.config.YamlConfigService;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.text.ItemGrouper;
import org.stt.time.DurationRounder;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ResourceBundle;

@Module
public class JFXModule {
    private JFXModule() {
    }

    @Provides
    static ReportWindowController createReportWindowBuilder(TimeTrackingItemQueries timeTrackingItemQueries,
                                                            DurationRounder durationRounder,
                                                            ItemGrouper itemGrouper,
                                                            ResourceBundle localization,
                                                            YamlConfigService yamlConfig) {
        return new ReportWindowController(
                localization,
                timeTrackingItemQueries,
                durationRounder,
                itemGrouper,
                yamlConfig.getConfig().getReportWindowConfig());
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
