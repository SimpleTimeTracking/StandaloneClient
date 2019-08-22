package org.stt.gui.jfx

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import org.stt.update.UpdateChecker
import java.awt.Desktop
import java.io.IOException
import java.io.UncheckedIOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named

class InfoController @Inject
constructor(private val localization: ResourceBundle,
            private val updateChecker: UpdateChecker,
            @Named("glyph") private val fontAwesome: Font,
            @Named("version") private val appVersion: String,
            @Named("commit hash") private val commitHash: String,
            private val executorService: ExecutorService) {
    private var panel: BorderPane? = null

    @FXML
    private lateinit var versionLabel: Label
    @FXML
    private lateinit var hashValue: Label
    @FXML
    private lateinit var updateArea: VBox

    @FXML
    fun initialize() {
        versionLabel.text = appVersion
        hashValue.text = commitHash
    }

    fun getPanel(): Pane? {
        loadAndInjectFXML()
        return panel
    }

    private fun loadAndInjectFXML() {
        val loader = FXMLLoader(javaClass.getResource(
                "/org/stt/gui/jfx/InfoPanel.fxml"), localization)
        loader.setController(this)
        try {
            panel = loader.load<BorderPane>()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    @FXML
    private fun checkForUpdate() {
        val progressIndicator = ProgressIndicator()
        progressIndicator.setMaxSize(40.0, 40.0)
        updateArea.children[0] = progressIndicator
        updateChecker.queryNewerVersion()
                .handleAsync<Any>({ a, t ->
                    if (t != null) {
                        updateArea.children[0] = Label(t.message)
                    }
                    updateArea.children[0] =
                            if (a != null) {
                                Label(String.format(localization.getString("info.newerVersion"), a), Glyph.glyph(fontAwesome, Glyph.ANGLE_DOUBLE_UP))
                            } else {
                                Label(localization.getString("info.upToDate"), Glyph.glyph(fontAwesome, Glyph.CHECK))
                            }
                    null
                }, { Platform.runLater(it) })
    }

    @FXML
    private fun openHomepage() {
        executorService.execute {
            try {
                Desktop.getDesktop().browse(URI(URL_PROJECT))
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, "Couldn't open homepage", ex)
            } catch (ex: URISyntaxException) {
                LOG.log(Level.SEVERE, "Couldn't open homepage", ex)
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(InfoController::class.java.name)
        const val URL_PROJECT = "https://github.com/SimpleTimeTracking/StandaloneClient/"
    }
}
