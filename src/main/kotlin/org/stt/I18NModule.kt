package org.stt


import dagger.Module
import dagger.Provides
import java.util.*

@Module
class I18NModule {

    @Provides
    fun provideResourceBundle(): ResourceBundle = ResourceBundle.getBundle("org.stt.gui.Application")
}
