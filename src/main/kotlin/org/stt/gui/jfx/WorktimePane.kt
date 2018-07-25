package org.stt.gui.jfx

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.listener.Handler
import org.stt.event.TimePassedEvent
import org.stt.gui.jfx.binding.STTBindings
import org.stt.model.ItemModified
import org.stt.query.WorkTimeQueries
import java.time.Duration
import java.util.*
import java.util.concurrent.Callable
import javax.inject.Inject

/**
 * Shows week worktime and day work-/overtime.
 * (First update is done after a second, to prevent blocking opening the UI)
 */
class WorktimePane @Inject
constructor(private val i18n: ResourceBundle,
            eventbus: MBassador<Any>,
            private val workTimeQueries: WorkTimeQueries) : FlowPane() {
    private val remainingWorktime = SimpleObjectProperty(Duration.ZERO)
    private val weekWorktime = SimpleObjectProperty(Duration.ZERO)

    init {
        eventbus.subscribe(this)

        build()
    }

    private fun build() {
        hgap = 20.0
        padding = Insets(2.0)
        val elements = children
        val remainingWorktimeToday = Label()
        val workOrOvertime = Bindings.createObjectBinding<Duration>(Callable { remainingWorktime.get().abs() }, remainingWorktime)
        remainingWorktimeToday.textProperty().bind(STTBindings.formattedDuration(workOrOvertime))
        val weekWorktimeLabel = Label()
        weekWorktimeLabel.textProperty().bind(STTBindings.formattedDuration(weekWorktime))

        val workOrOvertimeLabel = Label()
        workOrOvertimeLabel.textProperty().bind(
                Bindings.createStringBinding(
                        Callable { i18n.getString(if (remainingWorktime.get().isNegative) "overtimeToday" else "remainingWorktimeToday") },
                        remainingWorktime)
        )

        elements.add(hbox(4.0, workOrOvertimeLabel, remainingWorktimeToday))
        elements.add(hbox(4.0, Label(i18n.getString("weekWorktime")), weekWorktimeLabel))
    }

    private fun hbox(spacing: Double, vararg nodes: Node): HBox {
        val hBox = HBox(spacing)
        hBox.children.addAll(*nodes)
        return hBox
    }

    @Handler
    fun timePassed(event: TimePassedEvent) {
        updateWorktime()
    }

    @Handler
    fun onItemChange(event: ItemModified) {
        updateWorktime()
    }

    private fun updateWorktime() {
        remainingWorktime.value = workTimeQueries.queryRemainingWorktimeToday()
        weekWorktime.value = workTimeQueries.queryWeekWorktime()
    }

}
