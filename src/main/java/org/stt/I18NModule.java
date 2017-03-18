package org.stt;


import dagger.Module;
import dagger.Provides;

import java.util.ResourceBundle;

@Module
public class I18NModule {
    private I18NModule() {
    }

    @Provides
    static ResourceBundle provideResourceBundle() {
        return ResourceBundle.getBundle("org.stt.gui.Application");
    }
}
