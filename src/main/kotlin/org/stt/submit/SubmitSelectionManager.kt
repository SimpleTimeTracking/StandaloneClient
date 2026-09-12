package org.stt.submit

import org.stt.model.TimeTrackingItem
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitSelectionManager @Inject
constructor() {

    private val selections: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()

    fun isSelected(connectorId: String, item: TimeTrackingItem): Boolean {
        return selections[connectorId]?.contains(itemKey(item)) == true
    }

    fun setSelected(connectorId: String, item: TimeTrackingItem, selected: Boolean) {
        val key = itemKey(item)
        if (selected) {
            selections.computeIfAbsent(connectorId) { mutableSetOf() }.add(key)
        } else {
            selections[connectorId]?.remove(key)
        }
    }

    fun getSelectedItems(connectorId: String): Set<String> {
        return selections[connectorId]?.toSet() ?: emptySet()
    }

    fun clearSelection(connectorId: String) {
        selections.remove(connectorId)
    }

    private fun itemKey(item: TimeTrackingItem): String {
        val raw = "${item.activity}|${item.start}|${item.end}"
        return Base64.getEncoder().encodeToString(raw.toByteArray())
    }
}