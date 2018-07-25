package org.stt.gui

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.stage.Stage
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.listener.Handler
import org.controlsfx.dialog.ExceptionDialog
import org.stt.Service
import org.stt.StopWatch
import org.stt.event.ShuttingDown
import org.stt.event.TimePassedEvent
import org.stt.gui.jfx.MainWindowController
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.logging.Level
import java.util.logging.Logger

class UIMain : Application() {

    private val servicesToShutdown = CopyOnWriteArrayList<Service>()
    private var eventBus: MBassador<Any>? = null
    private lateinit var mainWindowController: MainWindowController
    private lateinit var executorService: ExecutorService

    override fun init() {
        val rootLogger = Logger.getLogger("")
        rootLogger.handlers[0].level = Level.FINEST
        Logger.getLogger("org.stt").level = Level.FINEST
        LOG.info("Starting STT in UI mode")

        LOG.info("Starting injector")
        val uiApplication = DaggerUIApplication.builder().build()

        executorService = uiApplication.executorService()
        startEventBus(uiApplication)

        startService(uiApplication.configService())
        startService(uiApplication.backupCreator())
        startService(uiApplication.itemLogService())

        LOG.info("init() done")
        mainWindowController = uiApplication.mainWindow()
    }

    private fun startEventBus(uiApplication: UIApplication) {
        LOG.info("Setting up event bus")
        eventBus = uiApplication.eventBus()
        eventBus!!.subscribe(this)
    }

    @Handler(priority = -999)
    fun shutdown(request: ShuttingDown) {
        LOG.info("Shutting down")
        try {
            Collections.reverse(servicesToShutdown)
            for (service in servicesToShutdown) {
                LOG.info("Stopping " + service.javaClass.simpleName)
                service.stop()
            }
            executorService.shutdown()
        } finally {
            Platform.exit()
        }
    }

    private fun startService(serviceInstance: Service) {
        val stopWatch = StopWatch(serviceInstance.javaClass.simpleName)
        LOG.info("Starting " + serviceInstance.javaClass.simpleName)
        serviceInstance.start()
        servicesToShutdown.add(serviceInstance)
        stopWatch.stop()
    }

    override fun start(primaryStage: Stage) {
        listOf("/Logo32.png", "/Logo64.png", "/Logo.png")
                .map { javaClass.getResourceAsStream(it) }
                .map { Image(it) }
                .forEach { primaryStage.icons.add(it) }
        Thread.currentThread().setUncaughtExceptionHandler { _, e ->
            LOG.log(Level.SEVERE, "Uncaught exception", e)
            eventBus!!.publish(e)
            ExceptionDialog(e).show()
        }
        LOG.info("Showing window")
        mainWindowController.show(primaryStage)

        scheduleOneUpdatePerSecond()
        LOG.fine("Window is now shown")
    }

    private fun scheduleOneUpdatePerSecond() {
        Timer(true).schedule(object : TimerTask() {
            override fun run() {
                Platform.runLater { eventBus!!.publish(TimePassedEvent()) }
            }
        }, 0, 1000)
    }

    companion object {
        /**
         * Set to true to add debug graphics
         */
        val DEBUG_UI = false
        private val LOG = Logger.getLogger(UIMain::class.java
                .name)

        @JvmStatic
        fun main(args: Array<String>) {
            LOG.info("START")
            Application.launch(UIMain::class.java, *args)
        }
    }
}
