package org.stt.gui.jfx

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import javafx.scene.control.Hyperlink
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import org.stt.config.ActivitiesConfig
import org.stt.text.ItemGrouper
import java.awt.Desktop
import java.io.IOException
import java.io.UncheckedIOException
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.regex.Pattern
import java.util.stream.Stream
import javax.inject.Named
import javax.inject.Singleton

private val URL_PATTERN = Pattern.compile("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

typealias ActivityTextDisplayProcessor = (Stream<Any>) -> Stream<Any>

@Module
abstract class JFXModule {

    @Multibinds
    abstract fun activityMappers(): Set<ActivityTextDisplayProcessor>

    @Module
    companion object {

        @Singleton
        @Provides
        @JvmStatic
        fun stageProvider(): Stage {
            return Stage()
        }

        @Provides
        @Named("activityToText")
        @JvmStatic
        fun provideActivityToTextMapper(mappers: Set<@JvmSuppressWildcards ActivityTextDisplayProcessor>): ActivityTextDisplayProcessor =
                {
                    var stream = it
                    for (mapper in mappers) {
                        stream = mapper(stream)
                    }
                    stream
                }

        @Provides
        @IntoSet
        @JvmStatic
        fun groupingMapper(activitiesConfig: ActivitiesConfig, grouper: @JvmSuppressWildcards ItemGrouper): ActivityTextDisplayProcessor =
                {
                    var first = true
                    it.flatMap {
                        if (!first) {
                            return@flatMap Stream.of(it)
                        }
                        first = false
                        if (it is String && activitiesConfig.isGroupItems) {
                            return@flatMap dissect(grouper, it)
                        }
                        Stream.of(it)
                    }
                }

        private fun dissect(grouper: ItemGrouper, o: String): Stream<*> {
            val result = ArrayList<Any>()
            val groups = grouper(o)
            for (i in groups.indices) {
                if (i > 0) {
                    result.add(" ")
                }
                val group = groups[i]
                val last = i >= groups.size - 1
                if (last) {
                    result.add(group.content)
                } else {
                    val text = Text(group.content)
                    text.styleClass.add("reportGroup$i")
                    result.add("\u2768")
                    result.add(text)
                    result.add("\u2769")
                }
            }
            return result.stream()
        }

        @Provides
        @IntoSet
        @JvmStatic
        fun hyperlinkMapper(executorService: ExecutorService): ActivityTextDisplayProcessor =
                {
                    it.flatMap {
                        return@flatMap if (it is String) {
                            toHyperlink(executorService, it)
                        } else {
                            Stream.of<Any>(it)
                        }
                    }
                }

        private fun toHyperlink(executorService: ExecutorService, activity: String): Stream<*> {
            val result = ArrayList<Any>()
            val matcher = URL_PATTERN.matcher(activity)
            var index = 0
            while (matcher.find(index)) {
                val preamble = activity.substring(index, matcher.start())
                if (!preamble.isEmpty()) {
                    result.add(preamble)
                }
                val uri = activity.substring(matcher.start(), matcher.end())
                val hyperlink = Hyperlink(uri)
                hyperlink.setOnAction {
                    executorService.submit {
                        try {
                            Desktop.getDesktop().browse(URI.create(uri))
                        } catch (e: IOException) {
                            throw UncheckedIOException(e)
                        }
                    }
                }
                result.add(hyperlink)
                index = matcher.end()
            }
            result.add(activity.substring(index))
            return result.stream()
        }

        @Provides
        @Named("glyph")
        @JvmStatic
        fun provideFont(): Font {
            try {
                JFXModule::class.java.getResourceAsStream("/fontawesome-webfont.ttf").use { fontStream -> return Font.loadFont(fontStream, 0.0) }
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }

        }
    }
}
