package org.stt.text

import org.stt.IntRange
import org.stt.StopWatch
import org.stt.config.CommonPrefixGrouperConfig
import org.stt.query.TimeTrackingItemQueries
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Learns common prefixes and uses them to determine groups.
 * Note that items are split at 'space' unless the resulting subgroup would have less than
 * 3 characters, in which case the group gets expanded.
 */
@Singleton
class CommonPrefixGrouper @Inject constructor(private val queries: TimeTrackingItemQueries,
                                              private val config: CommonPrefixGrouperConfig) : ItemGrouper, ExpansionProvider {

    private var initialized: Boolean = false
    private val root = PrefixTree()

    override fun invoke(text: String): List<Group> {
        checkInitialized()
        return GroupHelper(text, root).parse()
    }

    private fun checkInitialized() {
        if (initialized) {
            return
        }
        initialized = true

        val stopWatch = StopWatch("Item grouper")
        queries.queryAllItems()
                .map { it.activity }
                .forEach { insert(it) }

        config.baseLine.forEach { insert(it) }


        stopWatch.stop()
    }

    fun insert(item: String) {
        var node = root
        var i = 0
        val n = item.length

        val chars = item.toCharArray()
        while (i < n) {
            val child = node.child(chars[i])
            if (child != null) {
                node = child
                i++
            } else {
                break
            }
        }
        while (i < n) {
            val newChild = PrefixTree()
            node.child(chars[i], newChild)
            i++
            node = newChild
        }
        node.child(null, null)
    }


    override fun getPossibleExpansions(text: String): List<String> {
        checkInitialized()

        val chars = text.toCharArray()
        var node: PrefixTree? = root
        var i = 0
        val n = chars.size
        while (i < n && node != null) {
            node = node.child(chars[i])
            i++
        }
        return if (node == null)
            emptyList()
        else
            node.allChildren()
                    .map { entry ->
                        var tree: PrefixTree? = entry.value
                        val current = StringBuilder()
                        current.append(entry.key)
                        while (tree != null && tree.numChildren() == 1) {
                            val childChar = tree.anyChild()
                            tree = tree.child(childChar)
                            if (childChar != null) {
                                current.append(childChar)
                            }
                        }
                        current.toString()
                    }
                    .toList()
    }

    override fun toString(): String {
        return ""
    }

    private class PrefixTree {
        private var child: MutableMap<Char?, PrefixTree?>? = null

        internal fun child(c: Char?): PrefixTree? {
            if (child == null) {
                child = HashMap()
            }
            return child!![c]
        }

        internal fun child(c: Char?, newChild: PrefixTree?) {
            if (child == null) {
                child = HashMap()
            }
            child!![c] = newChild
        }

        internal fun numChildren(): Int {
            return if (child == null) 0 else child!!.size
        }

        internal fun anyChild(): Char? {
            return child!!.keys.iterator().next()
        }

        internal fun allChildren(): Collection<Map.Entry<Char?, PrefixTree?>> {
            return if (child == null) emptySet() else child!!.entries
        }
    }

    private class GroupHelper internal constructor(private val text: String, private var node: PrefixTree?) {
        private val groups = ArrayList<Group>()
        private val chars: CharArray
        private val n: Int
        private var i = 0
        private var start = 0
        private var lastGood: Int = 0

        init {
            this.chars = text.toCharArray()
            this.n = chars.size
        }

        fun parse(): List<Group> {
            while (i < n && node != null) {
                lastGood = start
                parseToNextBranch()
                setLastGoodToNextWhitespace()
                i = Math.max(i, lastGood)
                skipWhitespace()
                groups.add(Group(Type.MATCH, text.substring(start, lastGood), IntRange(start, lastGood)))
                start = i
            }
            if (start < n) {
                groups.add(Group(Type.REMAINDER, text.substring(start, n), IntRange(start, n)))
            }
            return groups
        }

        private fun skipWhitespace() {
            while (i < n && Character.isWhitespace(chars[i])) {
                if (node != null) {
                    node = node!!.child(chars[i])
                }
                i++
            }
        }

        private fun setLastGoodToNextWhitespace() {
            do {
                if (lastGood >= i && node != null) {
                    node = node!!.child(chars[lastGood])
                }
                lastGood++
            } while (lastGood < n && (!Character.isWhitespace(chars[lastGood]) || lastGood - start < MINIMUM_GROUP_LENGTH))
        }

        private fun parseToNextBranch() {
            do {
                if (!Character.isWhitespace(chars[i])) {
                    lastGood = i
                }
                node = node!!.child(chars[i])
                i++
            } while (i < n && node != null && node!!.numChildren() <= 1)
        }
    }

    companion object {
        val MINIMUM_GROUP_LENGTH = 3
    }
}
