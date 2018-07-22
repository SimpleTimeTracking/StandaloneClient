package org.stt.command

import net.engio.mbassy.bus.BusRuntime
import net.engio.mbassy.bus.IMessagePublication
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.bus.common.PubSubSupport
import org.stt.model.ItemDeleted
import org.stt.model.ItemInserted
import org.stt.model.ItemReplaced
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemPersister
import org.stt.query.Criteria
import org.stt.query.TimeTrackingItemQueries
import org.stt.time.DateTimes
import java.util.*
import java.util.Objects.requireNonNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Command "side" of tracking activities
 */
@Singleton
class Activities @Inject
constructor(private val persister: ItemPersister,
            private val queries: TimeTrackingItemQueries,
            publisher: Optional<MBassador<Any>>) : CommandHandler {

    private val publisher: PubSubSupport<Any> = publisher.map { it as PubSubSupport<Any> }.orElseGet { DoNotPublish() }

    override fun addNewActivity(command: NewActivity) {
        requireNonNull(command)
        val newItem = command.newItem
        var potentialItemToReplace = ongoingItemThatWouldEnd(newItem)
        if (potentialItemToReplace == null) {
            potentialItemToReplace = itemWithEditedActivity(newItem)
        }

        if (potentialItemToReplace != null) {
            persister.replace(potentialItemToReplace, command.newItem)
            publisher.publish(ItemReplaced(potentialItemToReplace, command.newItem))
        } else {
            persister.persist(command.newItem)
            publisher.publish(ItemInserted(command.newItem))
        }
    }

    private fun itemWithEditedActivity(newItem: TimeTrackingItem): TimeTrackingItem? {
        val criteria = Criteria()
                .withStartsAt(newItem.start)
        newItem.end?.let { criteria.withEndsAt(it) }
        return queries.queryItems(criteria).findAny().orElse(null)
    }

    private fun ongoingItemThatWouldEnd(newItem: TimeTrackingItem): TimeTrackingItem? {
        return queries.lastItem?.let {
            if (it.end == null && newItem.start == it.start) it else null
        }
    }

    override fun endCurrentActivity(command: EndCurrentItem) {
        requireNonNull(command)
        queries.ongoingItem?.let {
            val derivedItem = it.withEnd(command.endAt)
            persister.replace(it, derivedItem)
            publisher.publish(ItemReplaced(it, derivedItem))
        }
    }

    override fun removeActivity(command: RemoveActivity) {
        requireNonNull(command)
        persister.delete(command.itemToDelete)
        publisher.publish(ItemDeleted(command.itemToDelete))
    }

    override fun removeActivityAndCloseGap(command: RemoveActivity) {
        requireNonNull(command)
        val adjacentItems = queries.getAdjacentItems(command.itemToDelete)
        val previous = adjacentItems.previousItem
        val next = adjacentItems.nextItem

        if (previous != null) {
            if (next != null && previousAndNextActivitiesMatch(previous, next)) {
                val replaceAllWith = next.end?.let { previous.withEnd(it) } ?: previous.withPendingEnd()
                persister.persist(replaceAllWith)
                publisher.publish(ItemInserted(replaceAllWith))
                return
            }
            if (DateTimes.isOnSameDay(previous.start, command.itemToDelete.start) && command.itemToDelete.end == null) {
                val replaceAllWith = previous.withPendingEnd()
                persister.persist(replaceAllWith)
                publisher.publish(ItemInserted(replaceAllWith))
                return
            }
        }
        removeActivity(command)
    }

    private fun previousAndNextActivitiesMatch(previousItem: TimeTrackingItem, nextItem: TimeTrackingItem): Boolean {
        return previousItem.activity == nextItem.activity
    }

    override fun resumeActivity(command: ResumeActivity) {
        requireNonNull(command)
        val resumedItem = command.itemToResume
                .withPendingEnd()
                .withStart(command.beginningWith)
        persister.persist(resumedItem)
        publisher.publish(ItemInserted(resumedItem))
    }

    override fun resumeLastActivity(command: ResumeLastActivity) {
        requireNonNull(command)

        val lastTimeTrackingItem = queries.lastItem
        if (lastTimeTrackingItem?.end != null) {
            val resumedItem = lastTimeTrackingItem.withPendingEnd().withStart(command.resumeAt)
            persister.persist(resumedItem)
            publisher.publish(ItemInserted(resumedItem))
        }
    }

    override fun bulkChangeActivity(itemsToChange: Collection<TimeTrackingItem>, activity: String) {
        requireNonNull(itemsToChange)
        requireNonNull(activity)
        val updatedItems = persister.updateActivitities(itemsToChange, activity)
        updatedItems.stream()
                .map { updatedItem -> ItemReplaced(updatedItem.original, updatedItem.updated) }
                .forEach { publisher.publish(it) }
    }

    private class DoNotPublish : PubSubSupport<Any> {
        override fun subscribe(listener: Any) {
            throw IllegalStateException()
        }

        override fun unsubscribe(listener: Any): Boolean {
            throw IllegalStateException()
        }

        override fun publish(message: Any): IMessagePublication? {
            return null
        }

        override fun getRuntime(): BusRuntime {
            throw IllegalStateException()
        }
    }
}