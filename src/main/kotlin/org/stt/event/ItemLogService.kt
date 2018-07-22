package org.stt.event

import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.listener.Handler
import org.stt.Service
import org.stt.command.CommandFormatter
import org.stt.model.ItemDeleted
import org.stt.model.ItemInserted
import org.stt.model.ItemReplaced
import org.stt.model.TimeTrackingItem
import org.stt.time.DateTimes
import java.io.PrintWriter
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by dante on 20.03.15.
 */
class ItemLogService @Inject
constructor(@Named("itemLog") val out: PrintWriter,
            val eventBus: MBassador<Any>,
            val formatter: CommandFormatter) : Service {

    @Handler
    fun itemInserted(event: ItemInserted) = log("inserted", event.newItem)

    private fun log(eventType: String, item: TimeTrackingItem) {
        val command = formatter.asNewItemCommandText(item)
        val outputLine = StringBuilder()
        addCurrentTimeTo(outputLine)
        outputLine.append(", ").append(eventType).append(": ")
        outputLine.append(command)
        out.println(outputLine)
    }

    private fun addCurrentTimeTo(outputLine: StringBuilder) =
            outputLine.append(DateTimes.DATE_TIME_FORMATTER_YYYY_MM_DD_HH_MM_SS.format(LocalDateTime.now()))

    @Handler
    fun itemDeleted(event: ItemDeleted) = log("deleted", event.deletedItem)

    @Handler
    fun itemReplaced(event: ItemReplaced) {
        log("before_update", event.beforeUpdate)
        log("after_update", event.afterUpdate)
    }

    @Throws(Exception::class)
    override fun start() = eventBus.subscribe(this)

    override fun stop() = out.close()
}
