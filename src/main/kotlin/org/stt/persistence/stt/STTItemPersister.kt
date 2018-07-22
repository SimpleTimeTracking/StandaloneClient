package org.stt.persistence.stt

import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemPersister
import java.io.*
import java.util.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Writes [TimeTrackingItem]s to a new line. Multiline comments get joined
 * into one line: line endings \r and \n get replaced by the string \r and \n
 * respectively.
 */
@Singleton
class STTItemPersister @Inject
constructor(@STTFile val readerProvider: Provider<Reader>,
            @STTFile val writerProvider: Provider<Writer>) : ItemPersister {

    private val converter = STTItemConverter()

    override fun persist(itemToInsert: TimeTrackingItem) {
        val stringWriter = StringWriter()
        val providedReader: Reader = readerProvider.get()
        try {
            STTItemReader(providedReader).use { `in` ->
                STTItemWriter(stringWriter).use { out ->
                    InsertHelper(`in`, out, itemToInsert).performInsert()
                }
                rewriteFileWith(stringWriter.toString())
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    override fun replace(item: TimeTrackingItem, with: TimeTrackingItem) {
        delete(item)
        persist(with)
    }

    override fun delete(item: TimeTrackingItem) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)

        val lineOfItemToDelete = converter.timeTrackingItemToLine(item)
        BufferedReader(readerProvider.get()).useLines {
            it.filter { it != lineOfItemToDelete }.forEach { printWriter.println(it) }
        }
        rewriteFileWith(stringWriter.toString())
    }

    override fun updateActivitities(itemsToUpdate: Collection<TimeTrackingItem>, newActivity: String): Collection<ItemPersister.UpdatedItem> {
        try {
            STTItemReader(readerProvider.get()).use { `in` ->
                StringWriter().use { sw ->
                    STTItemWriter(sw).use { out ->
                        val matchingItems = HashSet(itemsToUpdate)
                        val updatedItems = ArrayList<ItemPersister.UpdatedItem>()
                        while (true) {
                            `in`.read()?.let {
                                val toWrite =
                                        if (matchingItems.contains(it)) {
                                            val updateItem = it.withActivity(newActivity)
                                            updatedItems.add(ItemPersister.UpdatedItem(it, updateItem))
                                            updateItem
                                        } else it
                                out.write(toWrite)
                            } ?: break
                        }
                        rewriteFileWith(sw.toString())
                        return updatedItems
                    }
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    private fun rewriteFileWith(content: String) {
        writerProvider.get().use {
            it.write(content)
        }
    }
}
