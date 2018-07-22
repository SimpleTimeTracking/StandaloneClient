package org.stt.persistence

import org.stt.model.TimeTrackingItem

interface ItemPersister {

    /**
     * Writes the given item.
     *
     *
     * If the new item has no end time:
     *
     *  * If the item.start is before any other item's start time, the existing
     * items will be removed.
     *  * If an item is not ended yet when the new item is written, it's end
     * time will be set to the new item's start time.
     *
     *
     *
     * @param item the item to persist. If it already exists, it will be
     * overwritten so the caller has to take care
     */
    fun persist(item: TimeTrackingItem)

    /**
     * Replaces the given item with a new one.
     *
     *
     * This is equivalent to calling [.delete] and
     * [.persist] but may potentially be faster
     *
     */
    fun replace(item: TimeTrackingItem, with: TimeTrackingItem)

    /**
     * @param item the item to delete. If the item does not already exist, just
     * does nothing
     */
    fun delete(item: TimeTrackingItem)

    fun updateActivitities(itemsToUpdate: Collection<TimeTrackingItem>, newActivity: String): Collection<UpdatedItem>

    class UpdatedItem(val original: TimeTrackingItem, val updated: TimeTrackingItem)
}
