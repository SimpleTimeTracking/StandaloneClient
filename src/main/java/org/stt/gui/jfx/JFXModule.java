package org.stt.gui.jfx;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javax.inject.Named;
import javax.inject.Singleton;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Module
public abstract class JFXModule {
    private static final Pattern URL_PATTERN = Pattern.compile("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    private JFXModule() {
    }

    @Singleton
    @Provides
    static Stage stageProvider() {
        return new Stage();
    }

    @Multibinds
    abstract Set<ActivityTextDisplayProcessor> activityMappers();

    @Provides
    @Named("activityToText")
    static ActivityTextDisplayProcessor provideActivityToTextMapper(Set<ActivityTextDisplayProcessor> mappers) {
        return o -> {
            Stream<Object> stream = Stream.of(o);
            for (Function<Object, Stream<Object>> mapper : mappers) {
                stream = stream.flatMap(mapper);
            }
            return stream;
        };
    }

    @Provides
    @IntoSet
    static ActivityTextDisplayProcessor hyperlinkMapper(ExecutorService executorService) {
        return o -> {
            if (o instanceof String) {
                List<Object> result = new ArrayList<>();
                String string = (String) o;
                Matcher matcher = URL_PATTERN.matcher(string);
                int index = 0;
                while (matcher.find(index)) {
                    String preamble = string.substring(index, matcher.start());
                    if (!preamble.isEmpty()) {
                        result.add(preamble);
                    }
                    String uri = string.substring(matcher.start(), matcher.end());
                    Hyperlink hyperlink = new Hyperlink(uri);
                    hyperlink.setOnAction(event -> executorService.submit(() -> {
                        try {
                            Desktop.getDesktop().browse(URI.create(uri));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }));
                    result.add(hyperlink);
                    index = matcher.end();
                }
                result.add(string.substring(index));
                return result.stream();
            }
            return Stream.of(o);
        };
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
