package org.stt.gui.jfx;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import javafx.stage.Stage;
import org.stt.YamlConfig;
import org.stt.analysis.ItemGrouper;
import org.stt.persistence.ItemReaderProvider;
import org.stt.search.ItemSearcher;
import org.stt.time.DurationRounder;

/**
 * Created by dante on 03.12.14.
 */
public class JFXModule extends AbstractModule {
    @Override
    protected void configure() {
    }

    @Provides
    private ReportWindowBuilder createReportWindowBuilder(ItemSearcher itemSearcher, ItemReaderProvider itemReaderProvider,
                                                          DurationRounder durationRounder, ItemGrouper itemGrouper, Provider<Stage> stageProvider,
                                                          YamlConfig yamlConfig) {
        return new ReportWindowBuilder(
                stageProvider, itemReaderProvider,
                itemSearcher, durationRounder, itemGrouper, yamlConfig.getConfig().getReportWindowConfig());
    }
}
