package org.stt.gui.jfx.binding

import javafx.beans.binding.ListBinding
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.RuleNode
import org.stt.Streams
import org.stt.command.CaseInsensitiveInputStream
import org.stt.grammar.EnglishCommandsBaseVisitor
import org.stt.grammar.EnglishCommandsLexer
import org.stt.grammar.EnglishCommandsParser
import org.stt.model.TimeTrackingItem
import java.util.*
import kotlin.streams.toList

class TimeTrackingListFilter(private val allItems: ObservableList<TimeTrackingItem>,
                             private val filterProperty: ObservableValue<String>,
                             private val filterDuplicates: Boolean) : ListBinding<TimeTrackingItem>() {

    init {
        bind(allItems, filterProperty)
    }

    override fun computeValue(): ObservableList<TimeTrackingItem> {
        val result = createFilteredList()
        return FXCollections.observableList(result)
    }

    private fun createFilteredList(): List<TimeTrackingItem> {
        val result: List<TimeTrackingItem>
        val filter = filterProperty.value.toLowerCase()
        if (filter.isEmpty()) {
            result = ArrayList(allItems)
        } else {
            val parsed = parseActivityPart(filter)
            var processingStream = allItems.stream()
                    .filter { item -> item.activity.toLowerCase().contains(parsed ?: filter) }
            if (filterDuplicates) {
                processingStream = processingStream.filter(Streams.distinctByKey { obj: TimeTrackingItem -> obj.activity })
            }
            result = processingStream.toList()
        }
        return result.reversed()
    }

    private fun parseActivityPart(filter: String): String? {
        val inputStream = CaseInsensitiveInputStream(filter)
        val lexer = EnglishCommandsLexer(inputStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = EnglishCommandsParser(tokenStream)
        val visitor = object : EnglishCommandsBaseVisitor<String>() {
            override fun shouldVisitNextChild(node: RuleNode?, currentResult: String?): Boolean {
                return currentResult == null
            }

            override fun visitItemWithComment(ctx: EnglishCommandsParser.ItemWithCommentContext): String {
                return ctx.text
            }
        }
        return visitor.visit(parser.command())
    }
}
