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
 * Activities must start with a Jira issue key (`ABC-123: ...`); items without a resolvable issue
 * key or without an end time are skipped with a warning. Before submitting, the days of the
 * selection are checked against existing Tempo worklogs and occupied days are skipped entirely.
 * Each successfully created worklog marks the item as submitted.
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
                skipped += itemsWithoutEnd
            }
            val sortedItems = validItems.sortedBy { it.start }
            val occupiedDays = findOccupiedDays(sortedItems.map { it.start.toLocalDate() })
            for (item in sortedItems) {
                submitSingleItem(item, occupiedDays)
            }
        }

        fun submitGrouped(selectedItems: List<ReportListItem>) {
            val groupsByDay = selectedItems
                    .flatMap { row -> row.backingItems.map { it.start.toLocalDate() to row } }
                    .groupBy({ it.first }, { it.second })
            val occupiedDays = findOccupiedDays(groupsByDay.keys)
            for ((day, rowsForDay) in groupsByDay) {
                if (day in occupiedDays) {
                    LOG.warning { "Skipping $day: day already has worklogs in Tempo." }
                    skipped += rowsForDay.sumOf { it.backingItems.size }
                    continue
                }
                submitGroupsOfDay(day, rowsForDay.distinct().sortedBy { row -> row.backingItems.minOf { it.start } })
            }
        }

        private fun submitGroupsOfDay(day: LocalDate, rows: List<ReportListItem>) {
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
                val duration = durationRounder.roundDuration(row.duration)
                if (duration.isZero) {
                    LOG.warning { "Skipping report group '${row.comment}': duration rounds to zero." }
                    skipped += row.backingItems.size
                    continue
                }
                val issueId = resolveIssueId(issueKey)
                if (issueId == null) {
                    skipped += row.backingItems.size
                    continue
                }
                cursor = durationRounder.roundDuration(cursor)
                val worklog = TempoWorklogInput(accountId, issueId, day.toString(),
                        formatTime(cursor), duration.seconds)
                if (createWorklog(worklog, "'${row.comment}'")) {
                    submitted++
                    row.backingItems.forEach { submitStatusTracker.markSubmitted(it, id) }
                } else {
                    failed++
                }
                cursor = cursor.plus(duration)
            }
        }

        private fun submitSingleItem(item: TimeTrackingItem, occupiedDays: Set<LocalDate>) {
            if (occupiedDays.contains(item.start.toLocalDate())) {
                LOG.warning { "Skipping item '$item': day ${item.start.toLocalDate()} already has worklogs in Tempo." }
                skipped++
                return
            }
            if (submitStatusTracker.isSubmitted(item, id)) {
                LOG.fine { "Skipping item '$item': already submitted to Tempo." }
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
            val dayStart = item.start.toLocalDate().atStartOfDay()
            val roundedStartOffset = durationRounder.roundDuration(Duration.between(dayStart, item.start))
            val worklog = TempoWorklogInput(accountId, issueId, item.start.toLocalDate().toString(),
                    formatTime(roundedStartOffset), roundedDuration.seconds)
            if (createWorklog(worklog, "'${item.activity}'")) {
                submitted++
                submitStatusTracker.markSubmitted(item, id)
            } else {
                failed++
            }
        }

        private fun findOccupiedDays(days: Collection<LocalDate>): Set<LocalDate> {
            if (days.isEmpty()) {
                return emptySet()
            }
            val from = days.min()
            val to = days.max()
            return try {
                tempoApiClient!!.getWorklogsForUser(accountId, from, to)
                        .mapNotNull { it.startDate?.let { date -> LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) } }
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
        private val ISSUE_KEY_REGEX = Regex("^([A-Z]+-\\d+)")

        fun extractIssueKey(activity: String): String? {
            return ISSUE_KEY_REGEX.find(activity.trim())?.groupValues?.get(1)
        }

        private fun formatTime(offsetFromMidnight: Duration): String {
            return LocalTime.MIDNIGHT.plus(offsetFromMidnight).format(DateTimeFormatter.ISO_LOCAL_TIME)
        }
    }
}
