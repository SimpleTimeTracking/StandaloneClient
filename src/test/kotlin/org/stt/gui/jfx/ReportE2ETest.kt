package org.stt.gui.jfx

import com.sun.javafx.application.PlatformImpl
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.stage.Stage
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.stt.BaseModule
import org.stt.gui.DaggerUIApplication
import org.testfx.api.FxRobot
import org.testfx.util.WaitForAsyncUtils
import java.nio.file.Files
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReportE2ETest {

    companion object {
        init {
            Locale.setDefault(Locale.GERMANY)

            System.setProperty("java.awt.headless", "true")
            System.setProperty("testfx.headless", "true")
            System.setProperty("glass.platform", "Monocle")
            System.setProperty("monocle.platform", "Headless")
            System.setProperty("prism.order", "sw")
            System.setProperty("prism.text", "t2k")
        }
    }

    private lateinit var robot: FxRobot
    private lateinit var stage: Stage
    private lateinit var activitiesController: ActivitiesController

    @Before
    fun setup() {
        PlatformImpl.startup { }

        val setupLatch = CountDownLatch(1)
        javafx.application.Platform.runLater {
            try {
                val tempFile = Files.createTempDirectory("stt-e2e").resolve("activities").toFile()
                tempFile.createNewFile()

                val app = DaggerUIApplication.builder()
                        .baseModule(BaseModule(sttFileOverride = tempFile))
                        .build()
                app.configService().start()
                val mainWindow = app.mainWindow()
                activitiesController = mainWindow.activitiesController

                stage = Stage()
                mainWindow.show(stage)
            } finally {
                setupLatch.countDown()
            }
        }
        setupLatch.await()

        robot = FxRobot()
        robot.targetWindow(stage)
    }

    @After
    fun teardown() {
        javafx.application.Platform.runLater { stage.close() }
    }

    @Test
    fun shouldShowGroupedReportForDayWithFourTopics() {
        val commands = listOf(
                "fix login bug from 09:00 to 10:00",
                "code review from 10:00 to 10:30",
                "team meeting from 10:30 to 11:30",
                "fix login bug from 11:30 to 12:00",
                "lunch from 12:00 to 13:00",
                "code review from 13:00 to 14:00",
                "team meeting from 14:00 to 14:30",
                "fix login bug from 14:30 to 15:30",
                "team meeting from 15:30 to 16:00",
                "code review from 16:00 to 17:00"
        )

        for (cmd in commands) {
            robot.interact(Runnable { activitiesController.commandText.replaceText(cmd) })
            WaitForAsyncUtils.waitForFxEvents()
            robot.clickOn("#insert")
            WaitForAsyncUtils.waitForFxEvents()
        }

        robot.clickOn("#reportTab")
        WaitForAsyncUtils.waitForFxEvents()
        WaitForAsyncUtils.sleep(1000L, TimeUnit.MILLISECONDS)

        val table = robot.lookup(".table-view").query<TableView<ReportController.ReportListItem>>()
        assertThat(table.items).hasSize(4)

        fun row(comment: String) = table.items.first { it.comment == comment }

        assertThat(row("fix login bug").duration).isEqualTo(Duration.ofHours(2).plusMinutes(30))
        assertThat(row("code review").duration).isEqualTo(Duration.ofHours(2).plusMinutes(30))
        assertThat(row("team meeting").duration).isEqualTo(Duration.ofHours(2))
        assertThat(row("lunch").duration).isEqualTo(Duration.ofHours(1))

        assertThat(robot.lookup("#totalDuration").query<Label>().text).contains("8")
        assertThat(robot.lookup("#effectiveDuration").query<Label>().text).contains("8")
        assertThat(robot.lookup("#breakDuration").query<Label>().text).contains("0")
        assertThat(robot.lookup("#uncoveredDuration").query<Label>().text).contains("0")
    }
}