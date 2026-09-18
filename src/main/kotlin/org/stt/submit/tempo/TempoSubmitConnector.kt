package org.stt.submit.tempo

import net.engio.mbassy.bus.MBassador
import org.stt.config.TempoConfig
import org.stt.config.TempoSummaryMode
import org.stt.connector.jira.JiraConnector
import org.stt.connector.jira.JiraConnectorException
import org.stt.event.NotifyUser
import org.stt.gui.jfx.ReportController.ReportListItem
import org.stt.model.TimeTrackingItem
import org.stt.reporting.SummingReportGenerator.Report
import org.stt.submit.SubmitConnector
import org.stt.submit.SubmitStatusTracker
import org.stt.time.DurationRounder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.logging.Logger

/**
 * Submits time tracking items as worklogs to Jira Tempo Cloud.
 *
 * Activities must start with a Jira issue key (`ABC-123: ...`, case-insensitive); the text after the
 * issue key is used as the worklog comment. Items without a resolvable issue key or without an end
 * time are skipped with a warning. Before submitting, existing Tempo worklogs are checked and only
 * items whose issue already has a worklog on that day are skipped. Each successfully created worklog
 * marks the item as submitted.
 */
class TempoSubmitConnector(
    private val tempoConfig: TempoConfig,
    private val jiraConnector: JiraConnector,
    private val durationRounder: DurationRounder,
    private val submitStatusTracker: SubmitStatusTracker,
    private val eventBus: Optional<MBassador<Any>>,
    private val tempoApiClient: TempoApiClient?
) : SubmitConnector {

    override val id: String = "tempo"

    override fun submitItems(items: List<TimeTrackingItem>) {
        if (tempoApiClient == null) {
            notifyUser("Tempo connector is not configured: set tempoToken in the tempo config section.")
            return
        }
        val run = SubmitRun()
        run.submitItems(items)
        run.notifySummary()
    }

    override fun submitSummary(report: Report, selectedItems: List<ReportListItem>) {
        if (tempoApiClient == null) {
            notifyUser("Tempo connector is not configured: set tempoToken in the tempo config section.")
            return
        }
        when (tempoConfig.summaryMode) {
            TempoSummaryMode.ITEMS -> submitItems(selectedItems.flatMap { it.backingItems })
            TempoSummaryMode.GROUPED -> {
                val run = SubmitRun()
                run.submitGrouped(selectedItems)
                run.notifySummary()
            }
        }
    }

    private inner class SubmitRun {
        var submitted = 0
        var skipped = 0
        var failed = 0
        private val accountId: String by lazy { fetchAccountId() }
        private val issueIdCache = HashMap<String, Long?>()

        fun submitItems(items: List<TimeTrackingItem>) {
            val validItems = items.filter { it.end != null }
            val itemsWithoutEnd = items.size - validItems.size
            if (itemsWithoutEnd > 0) {
                LOG.warning { "Skipping $itemsWithoutEnd item(s) without end time: items without end are never submitted." }
                notifyUser("Skipping $itemsWithoutEnd item(s) without end time: items without end are never submitted.")
                skipped += itemsWithoutEnd
            }
            val sortedItems = validItems.sortedBy { it.start }
            val occupiedIssueDays = findOccupiedIssueDays(sortedItems.map { it.start.toLocalDate() })
            for (item in sortedItems) {
                submitSingleItem(item, occupiedIssueDays)
            }
        }

        fun submitGrouped(selectedItems: List<ReportListItem>) {
            val (validRows, rowsWithOngoingItems) = selectedItems.partition { row ->
                row.backingItems.all { it.end != null }
            }
            for (row in rowsWithOngoingItems) {
                LOG.warning { "Skipping report group '${row.comment}': contains item(s) without end time: items without end are never submitted." }
                notifyUser("Skipping '${row.comment}': group contains item(s) without end time and cannot be submitted.")
                skipped += row.backingItems.size
            }
            val groupsByDay = validRows
                    .flatMap { row -> row.backingItems.map { it.start.toLocalDate() to row } }
                    .groupBy({ it.first }, { it.second })
            val occupiedIssueDays = findOccupiedIssueDays(groupsByDay.keys)
            for ((day, rowsForDay) in groupsByDay) {
                submitGroupsOfDay(day, rowsForDay.distinct().sortedBy { row -> row.backingItems.minOf { it.start } }, occupiedIssueDays)
            }
        }

        private fun submitGroupsOfDay(day: LocalDate, rows: List<ReportListItem>,
                                      occupiedIssueDays: Set<Pair<Long, LocalDate>>) {
            var cursor: Duration = tempoConfig.workdayStart
            for (row in rows) {
                if (row.backingItems.isEmpty()) {
                    LOG.warning { "Skipping report group '${row.comment}': no backing items to submit." }
                    skipped++
                    continue
                }
                val issueKey = extractIssueKey(row.comment)
                if (issueKey == null) {
                    LOG.warning { "Skipping report group '${row.comment}': no issue key found." }
                    notifyUser("Skipping '${row.comment}': no Jira issue key found.")
                    skipped += row.backingItems.size
                    continue
                }
                val issueId = resolveIssueId(issueKey)
                if (issueId == null) {
                    skipped += row.backingItems.size
                    continue
                }
                if (issueId to day in occupiedIssueDays) {
                    LOG.warning { "Skipping report group '${row.comment}': issue $issueKey already has worklogs in Tempo on $day." }
                    notifyUser("Skipping '${row.comment}': issue $issueKey already has worklogs in Tempo on $day.")
                    skipped += row.backingItems.size
                    continue
                }
                val duration = durationRounder.roundDuration(row.duration)
                if (duration.isZero) {
                    LOG.warning { "Skipping report group '${row.comment}': duration rounds to zero." }
                    skipped += row.backingItems.size
                    continue
                }
                cursor = durationRounder.roundDuration(cursor)
                val worklog = TempoWorklogInput(accountId, issueId, day.toString(),
                        formatTime(cursor), duration.seconds, extractWorklogComment(row.comment))
                if (createWorklog(worklog, "'${row.comment}'")) {
                    submitted++
                    row.backingItems.forEach { submitStatusTracker.markSubmitted(it, id) }
                } else {
                    failed++
                }
                cursor = cursor.plus(duration)
            }
        }

        private fun submitSingleItem(item: TimeTrackingItem,
                                     occupiedIssueDays: Set<Pair<Long, LocalDate>>) {
            if (submitStatusTracker.isSubmitted(item, id)) {
                LOG.fine { "Skipping item '$item': already submitted to Tempo." }
                return
            }
            val issueKey = extractIssueKey(item.activity)
            if (issueKey == null) {
                LOG.warning { "Skipping item '$item': no issue key found in activity." }
                notifyUser("Skipping '${item.activity}': no Jira issue key found.")
                skipped++
                return
            }
            val issueId = resolveIssueId(issueKey)
            if (issueId == null) {
                skipped++
                return
            }
            val day = item.start.toLocalDate()
            if (issueId to day in occupiedIssueDays) {
                LOG.warning { "Skipping item '$item': issue $issueKey already has worklogs in Tempo on $day." }
                notifyUser("Skipping '${item.activity}': issue $issueKey already has worklogs in Tempo on $day.")
                skipped++
                return
            }
            val end = item.end!!
            val roundedDuration = durationRounder.roundDuration(Duration.between(item.start, end))
            if (roundedDuration.isZero) {
                LOG.warning { "Skipping item '$item': duration rounds to zero." }
                notifyUser("Skipping '${item.activity}': duration rounds to zero.")
                skipped++
                return
            }
            val dayStart = day.atStartOfDay()
            val roundedStartOffset = durationRounder.roundDuration(Duration.between(dayStart, item.start))
            val worklog = TempoWorklogInput(accountId, issueId, day.toString(),
                    formatTime(roundedStartOffset), roundedDuration.seconds, extractWorklogComment(item.activity))
            if (createWorklog(worklog, "'${item.activity}'")) {
                submitted++
                submitStatusTracker.markSubmitted(item, id)
            } else {
                failed++
            }
        }

        private fun findOccupiedIssueDays(days: Collection<LocalDate>): Set<Pair<Long, LocalDate>> {
            if (days.isEmpty()) {
                return emptySet()
            }
            val from = days.min()
            val to = days.max()
            return try {
                tempoApiClient!!.getWorklogsForUser(accountId, from, to)
                        .mapNotNull { worklog ->
                            val issueId = worklog.issueId ?: return@mapNotNull null
                            val startDate = worklog.startDate
                                    ?.let { date -> LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }
                                    ?: return@mapNotNull null
                            issueId to startDate
                        }
                        .toSet()
            } catch (e: TempoClientException) {
                LOG.severe { "Could not query existing Tempo worklogs: ${e.message}" }
                notifyUser("Tempo: could not query existing worklogs: ${e.message}")
                emptySet()
            }
        }

        private fun resolveIssueId(issueKey: String): Long? {
            if (issueIdCache.containsKey(issueKey)) {
                return issueIdCache[issueKey]
            }
            val issueId = try {
                jiraConnector.getIssue(issueKey)?.id
            } catch (e: JiraConnectorException) {
                LOG.warning { "Could not retrieve issue '$issueKey': ${e.message}" }
                notifyUser("Skipping '$issueKey': could not retrieve issue from Jira.")
                null
            }
            if (issueId == null) {
                LOG.warning { "Issue '$issueKey' has no numeric id or does not exist." }
                notifyUser("Skipping '$issueKey': issue not found in Jira.")
            }
            issueIdCache[issueKey] = issueId
            return issueId
        }

        private fun createWorklog(worklog: TempoWorklogInput, description: String): Boolean {
            return try {
                tempoApiClient!!.createWorklog(worklog)
                LOG.info { "Created Tempo worklog for $description on ${worklog.startDate} at ${worklog.startTime} for ${worklog.timeSpentSeconds} seconds." }
                true
            } catch (e: TempoClientException) {
                LOG.severe { "Failed to create Tempo worklog for $description: ${e.message}" }
                notifyUser("Tempo: failed to submit $description: ${e.message}")
                false
            }
        }

        private fun fetchAccountId(): String {
            val myself = jiraConnector.getMyself()
                    ?: throw IllegalStateException("Could not determine Jira account id: not configured.")
            return myself.accountId
                    ?: throw IllegalStateException("Could not determine Jira account id: Jira returned no account id.")
        }

        fun notifySummary() {
            notifyUser("Tempo: $submitted submitted, $skipped skipped, $failed failed.")
        }
    }

    private fun notifyUser(message: String) {
        eventBus.ifPresent { it.publish(NotifyUser(message)) }
    }

    companion object {
        private val LOG = Logger.getLogger(TempoSubmitConnector::class.java.name)
        private val ISSUE_KEY_REGEX = Regex("^([A-Za-z]+-\\d+)\\s*[:;-]?\\s*(.*)$")

        /**
         * Extracts the Jira issue key from the start of the activity text,
         * case-insensitively, normalized to upper case.
         */
        fun extractIssueKey(activity: String): String? {
            return ISSUE_KEY_REGEX.find(activity.trim())?.groupValues?.get(1)?.uppercase()
        }

        /**
         * Extracts the text after the issue key as the worklog comment,
         * or null when the activity consists only of the issue key.
         */
        fun extractWorklogComment(activity: String): String? {
            val match = ISSUE_KEY_REGEX.find(activity.trim()) ?: return null
            return match.groupValues[2].trim().ifEmpty { null }
        }

        private fun formatTime(offsetFromMidnight: Duration): String {
            return LocalTime.MIDNIGHT.plus(offsetFromMidnight).format(DateTimeFormatter.ISO_LOCAL_TIME)
        }
    }
}
