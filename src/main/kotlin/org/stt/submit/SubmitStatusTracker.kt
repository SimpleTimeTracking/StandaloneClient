package org.stt.submit

import org.stt.Service
import org.stt.model.TimeTrackingItem
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SubmitStatusTracker @Inject
constructor(@Named("homePath") homePath: String) : Service {

    private val statusFile: File = File("$homePath/.stt", "submit-status")
    private val submitted: MutableMap<String, MutableMap<String, String>> = ConcurrentHashMap()

    override fun start() {
        statusFile.parentFile.mkdirs()
        if (statusFile.exists()) {
            Files.readAllLines(statusFile.toPath()).forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    val itemKey = parts[0]
                    val connectorId = parts[1]
                    val timestamp = parts[2]
                    submitted.computeIfAbsent(itemKey) { ConcurrentHashMap() }[connectorId] = timestamp
                }
            }
        }
        LOG.info("SubmitStatusTracker loaded ${submitted.size} item statuses")
    }

    override fun stop() {
        save()
    }

    private fun save() {
        val lines = submitted.flatMap { (itemKey, connectors) ->
            connectors.map { (connectorId, timestamp) ->
                "$itemKey|$connectorId|$timestamp"
            }
        }
        Files.write(statusFile.toPath(), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    fun isSubmitted(item: TimeTrackingItem): Boolean {
        val key = itemKey(item)
        return submitted.containsKey(key)
    }

    fun isSubmitted(item: TimeTrackingItem, connectorId: String): Boolean {
        val key = itemKey(item)
        return submitted[key]?.containsKey(connectorId) == true
    }

    fun markSubmitted(item: TimeTrackingItem, connectorId: String) {
        val key = itemKey(item)
        submitted.computeIfAbsent(key) { ConcurrentHashMap() }[connectorId] = LocalDateTime.now().toString()
    }

    fun getSubmitFraction(items: List<TimeTrackingItem>, connectorId: String): Float {
        if (items.isEmpty()) return 1.0f
        val submittedCount = items.count { isSubmitted(it, connectorId) }
        return submittedCount.toFloat() / items.size
    }

    fun requireNotSubmitted(item: TimeTrackingItem) {
        if (isSubmitted(item)) {
            throw IllegalStateException("Item is already submitted and cannot be modified: $item")
        }
    }

    private fun itemKey(item: TimeTrackingItem): String {
        val raw = "${item.activity}|${item.start}|${item.end}"
        return Base64.getEncoder().encodeToString(raw.toByteArray())
    }

    companion object {
        private val LOG = Logger.getLogger(SubmitStatusTracker::class.java.name)
    }
}