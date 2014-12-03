package org.stt;

import com.google.inject.AbstractModule;

import java.util.ResourceBundle;

/**
 * Created by dante on 03.12.14.
 */
public class I18NModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ResourceBundle.class).toInstance(ResourceBundle.getBundle("org.stt.gui.Application"));
    }
}
