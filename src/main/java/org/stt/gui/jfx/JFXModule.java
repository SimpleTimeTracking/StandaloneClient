package org.stt.gui.jfx;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.stt.config.ActivitiesConfig;
import org.stt.text.ItemGrouper;

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
            Stream<Object> stream = o;
            for (ActivityTextDisplayProcessor mapper : mappers) {
                stream = mapper.apply(stream);
            }
            return stream;
        };
    }

    @Provides
    @IntoSet
    static ActivityTextDisplayProcessor groupingMapper(ActivitiesConfig activitiesConfig, ItemGrouper grouper) {

        return in -> {
            Boolean[] first = {true};
            return in.flatMap(o -> {
                if (!first[0]) {
                    return Stream.of(o);
                }
                first[0] = false;
                if (o instanceof String && activitiesConfig.isGroupItems()) {
                    return dissect(grouper, (String) o);
                }
                return Stream.of(o);
            });
        };
    }

    private static Stream<?> dissect(ItemGrouper grouper, String o) {
        List<Object> result = new ArrayList<>();
        List<ItemGrouper.Group> groups = grouper.getGroupsOf(o);
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) {
                result.add(" ");
            }
            ItemGrouper.Group group = groups.get(i);
            boolean last = i >= groups.size() - 1;
            if (last) {
                result.add(group.content);
            } else {
                Text text = new Text(group.content);
                text.getStyleClass().add("reportGroup" + i);
                result.add("\u2768");
                result.add(text);
                result.add("\u2769");
            }
        }
        return result.stream();
    }

    @Provides
    @IntoSet
    static ActivityTextDisplayProcessor hyperlinkMapper(ExecutorService executorService) {
        return in -> in.flatMap(o -> {
            if (o instanceof String) {
                return toHyperlink(executorService, (String) o);
            }
            return Stream.of(o);
        });
    }

    private static Stream<?> toHyperlink(ExecutorService executorService, String activity) {
        List<Object> result = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(activity);
        int index = 0;
        while (matcher.find(index)) {
            String preamble = activity.substring(index, matcher.start());
            if (!preamble.isEmpty()) {
                result.add(preamble);
            }
            String uri = activity.substring(matcher.start(), matcher.end());
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
        result.add(activity.substring(index));
        return result.stream();
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
