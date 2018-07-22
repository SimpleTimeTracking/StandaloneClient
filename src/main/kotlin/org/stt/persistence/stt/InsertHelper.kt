package org.stt.persistence.stt

import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import org.stt.persistence.ItemWriter

internal class InsertHelper(val reader: ItemReader,
                            val writer: ItemWriter,
                            val itemToInsert: TimeTrackingItem) {
    private var lastReadItem: TimeTrackingItem? = null

    fun performInsert() {
        copyAllItemsEndingAtOrBeforeItemToInsert()
        lastReadItem?.let { this.adjustEndOfLastItemReadAndWrite(it) }
        writer.write(itemToInsert)
        skipAllItemsCompletelyCoveredByItemToInsert()
        lastReadItem?.let { this.adjustStartOfLastItemReadAndWrite(it) }
        copyRemainingItems()
    }

    private fun copyAllItemsEndingAtOrBeforeItemToInsert() {
        copyWhile { it.endsAtOrBefore(itemToInsert.start) }
    }

    private fun adjustStartOfLastItemReadAndWrite(item: TimeTrackingItem) {
        val itemToWrite = itemToInsert.end?.let { end -> if (end.isAfter(item.start)) item.withStart(end) else null }
                ?: item
        writer.write(itemToWrite)
    }

    private fun adjustEndOfLastItemReadAndWrite(item: TimeTrackingItem) {
        if (item.start.isBefore(itemToInsert.start)) {
            val itemBeforeItemToInsert = item
                    .withEnd(itemToInsert.start)
            writer.write(itemBeforeItemToInsert)
        }
    }

    private fun copyRemainingItems() {
        copyWhile { true }
    }

    private fun skipAllItemsCompletelyCoveredByItemToInsert() {
        var currentItem = lastReadItem
        while (currentItem != null && itemToInsert.endsSameOrAfter(currentItem)) {
            currentItem = reader.read()
        }
        lastReadItem = currentItem
    }

    private fun copyWhile(condition: (TimeTrackingItem) -> Boolean) {
        do {
            lastReadItem = reader.read()
            lastReadItem?.let { if (condition(it)) writer.write(it) else return }
        } while (lastReadItem != null)
    }
}
