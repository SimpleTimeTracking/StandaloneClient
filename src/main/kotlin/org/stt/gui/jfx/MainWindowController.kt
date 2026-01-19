package org.stt.gui.jfx

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.listener.Handler
import org.controlsfx.control.Notifications
import org.stt.event.NotifyUser
import org.stt.event.ShuttingDown
import org.stt.gui.UIMain
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject

class MainWindowController @Inject
internal constructor(private val localization: ResourceBundle,
                     private val activitiesController: ActivitiesController,
                     private val reportController: ReportController,
                     private val eventBus: MBassador<Any>,
                     private val settingsController: SettingsController,
                     private val infoController: InfoController) {

    private val rootNode: Parent

    @FXML
    private lateinit var activitiesTab: Tab
    @FXML
    private lateinit var reportTab: Tab
    @FXML
    private lateinit var mainTabPane: javafx.scene.control.TabPane
    @FXML
    private lateinit var settingsTab: Tab
    @FXML
    private lateinit var infoTab: Tab

    init {
        val loader = FXMLLoader(javaClass.getResource(
                "/org/stt/gui/jfx/MainWindow.fxml"), localization)
        loader.setController(this)

        try {
            rootNode = loader.load()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

        rootNode.stylesheets.add("org/stt/gui/jfx/STT.css")

        eventBus.subscribe(this)
    }

    @Handler
    fun onUserNotifactionRequest(event: NotifyUser) {
        Notifications.create().text(event.message).show()
    }

    @FXML
    fun initialize() {
        activitiesTab.content = activitiesController.node
        CompletableFuture.supplyAsync { reportController.panel }
                .thenAcceptAsync({ reportTab.content = it }, { Platform.runLater(it) })
                .handle { _, t ->
                    if (t != null) LOG.log(Level.SEVERE, "Error while building report controller", t)
                    t!!.message
                }
        CompletableFuture.supplyAsync { settingsController.panel }
                .thenAcceptAsync({ settingsTab.content = it }, { Platform.runLater(it) })
                .handle { _, t ->
                    if (t != null) LOG.log(Level.SEVERE, "Error while building settings controller", t)
                    t!!.message
                }

        CompletableFuture.supplyAsync { infoController.getPanel() }
                .thenAcceptAsync({ infoTab.content = it }, { Platform.runLater(it) })
                .handle { _, t ->
                    if (t != null) LOG.log(Level.SEVERE, "Error while building info controller", t)
                    t!!.message
                }
    }

    fun show(stage: Stage) {
        val scene = Scene(rootNode)

        stage.setOnCloseRequest { Platform.runLater { this.shutdown() } }
        // use an event filter on the scene so we catch key presses before focused controls consume them
        scene.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            when (event.code) {
                KeyCode.ESCAPE -> {
                    event.consume()
                    shutdown()
                }
                // Alt+R (or Meta+R on macOS) to switch to report tab
                KeyCode.R -> {
                    if (event.isAltDown || event.isMetaDown) {
                        event.consume()
                        try {
                            mainTabPane.selectionModel.select(reportTab)
                        } catch (e: Exception) {
                            LOG.log(Level.WARNING, "Error selecting report tab via hotkey", e)
                        }
                    }
                }
                KeyCode.UP -> {
                    if (reportTab.isSelected) {
                        event.consume()
                        try {
                            reportController.selectPreviousTrackedDay()
                        } catch (e: Exception) {
                            LOG.log(Level.WARNING, "Error selecting previous tracked day", e)
                        }
                    } else {
                        // explicitly consume UP when not on report tab to disable its behavior elsewhere
                        event.consume()
                    }
                }
                KeyCode.DOWN -> {
                    if (reportTab.isSelected) {
                        event.consume()
                        try {
                            reportController.selectNextTrackedDay()
                        } catch (e: Exception) {
                            LOG.log(Level.WARNING, "Error selecting next tracked day", e)
                        }
                    } else {
                        // explicitly consume DOWN when not on report tab to disable its behavior elsewhere
                        event.consume()
                    }
                }
                else -> {
                    // leave other keys (including LEFT/RIGHT) to default handlers
                }
            }
        }

        val applicationIcon = Image("/Logo.png", 32.0, 32.0, true, true)

        stage.icons.add(applicationIcon)
        stage.title = localization.getString("window.title")
        stage.scene = scene
        stage.sizeToScene()
        stage.show()
    }

    private fun shutdown() {
        eventBus.publish(ShuttingDown())
    }

    companion object {
        private val LOG = Logger.getLogger(UIMain::class.java
                .name)
    }
}
