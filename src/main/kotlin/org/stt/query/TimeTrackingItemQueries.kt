package org.stt.query

import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.listener.Handler
import org.stt.StopWatch
import org.stt.model.ItemModified
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import java.time.LocalDate
import java.util.*
import java.util.logging.Logger
import java.util.stream.Stream
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KProperty

@Singleton
class TimeTrackingItemQueries @Inject constructor(private val provider: Provider<ItemReader>,
                                                  eventbus: Optional<MBassador<Any>>) {
    private val log = Logger.getLogger(TimeTrackingItemQueries::class.java.simpleName)
    private var cache = Cache<MutableList<TimeTrackingItem>> @Synchronized {
        log.fine("Rebuilding cache")
        val stopWatch = StopWatch("Query cache rebuild")
        val updatedList = ArrayList<TimeTrackingItem>(2000)
        provider.get().use { reader ->
            while (true) {
                val item: TimeTrackingItem = reader.read() ?: return@use
                updatedList += item
            }
        }
        stopWatch.stop()
        updatedList
    }
    private val items: MutableList<TimeTrackingItem> by cache

    /**
     * Returns the item which is ongoing (even if it starts in the future). This is necessarily the last item.
     */
    val ongoingItem: TimeTrackingItem?
        get() = if (lastItem?.end == null) lastItem else null

    val lastItem: TimeTrackingItem? get() = items.lastOrNull()

    init {
        eventbus.ifPresent { bus -> bus.subscribe(this) }
    }

    @Handler(priority = Integer.MAX_VALUE)
    @Synchronized
    fun sourceChanged(event: ItemModified?) {
        cache.clear()
        log.fine("Clearing query cache")
    }

    /**
     * Returns the items coming directly before and directly after the give item.
     * There will be no gap between previousItem, forItem and nextItem
     */
    fun getAdjacentItems(forItem: TimeTrackingItem): AdjacentItems {
        val itemIndex = items.indexOf(forItem)
        var previous: TimeTrackingItem? = null
        if (itemIndex > 0) {
            val potentialPrevious = items[itemIndex - 1]
            if (potentialPrevious.end == forItem.start) {
                previous = potentialPrevious
            }
        }
        var next: TimeTrackingItem? = null
        if (itemIndex < items.size - 1) {
            val potentialNext = items[itemIndex + 1]
            if (forItem.end == potentialNext.start) {
                next = potentialNext
            }
        }
        return AdjacentItems(previous, next)
    }

    /**
     * @return a [Stream] containing all time tracking items, be sure to [Stream.close] it!
     */
    fun queryAllTrackedDays(): Stream<LocalDate> {
        return queryAllItems()
                .map { it.start }
                .map { it.toLocalDate() }
                .distinct()
    }

    /**
     * @return a [Stream] containing all time tracking items matching the given criteria, be sure to [Stream.close] it!
     */
    fun queryItems(criteria: Criteria): Stream<TimeTrackingItem> =
            queryAllItems().filter { criteria.matches(it) }

    /**
     * @return a [Stream] containing all time tracking items, be sure to [Stream.close] it!
     */
    fun queryAllItems(): Stream<TimeTrackingItem> = items.stream()


    class AdjacentItems(val previousItem: TimeTrackingItem?, val nextItem: TimeTrackingItem?)
}

class Cache<T>(private val updater: () -> T) {
    private var value: Any? = null
    private var cached = false

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (cached) return value as T
        value = updater()
        cached = true
        return value as T
    }

    fun clear() {
        cached = false
    }
}