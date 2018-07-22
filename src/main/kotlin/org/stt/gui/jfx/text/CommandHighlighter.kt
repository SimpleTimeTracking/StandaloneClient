package org.stt.gui.jfx.text

import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.fxmisc.richtext.StyleClassedTextArea
import org.stt.command.CaseInsensitiveInputStream
import org.stt.config.ReportConfig
import org.stt.grammar.EnglishCommandsBaseVisitor
import org.stt.grammar.EnglishCommandsLexer
import org.stt.grammar.EnglishCommandsParser
import org.stt.text.ItemGrouper
import org.stt.text.Type
import java.util.*
import javax.inject.Inject

class CommandHighlighter(private val itemGrouper: ItemGrouper,
                         private val config: ReportConfig,
                         private val textArea: StyleClassedTextArea) {
    private val visitor = Highlighter()

    fun update() {
        if (textArea.length == 0) {
            return
        }
        textArea.clearStyle(0, textArea.length)
        val text = textArea.text
        val input = CaseInsensitiveInputStream(text)
        val lexer = EnglishCommandsLexer(input)
        val tokenStream = CommonTokenStream(lexer)
        val parser = EnglishCommandsParser(tokenStream)
        parser.command().accept(visitor)
        val groups = itemGrouper(text).toTypedArray()
        for (i in 0 until groups.size - 1) {
            val group = groups[i]
            if (group.type == Type.MATCH) {
                textArea.setStyle(group.range.start, group.range.end, Arrays.asList("matchedGroup", "group$i"))
            }
        }
        textArea.style = "-fx-fill: red;"
    }

    private inner class Highlighter : EnglishCommandsBaseVisitor<Void>() {
        override fun visitDateTime(ctx: EnglishCommandsParser.DateTimeContext): Void? {
            markKeyWords(ctx)
            return null
        }

        override fun visitTimeFormat(ctx: EnglishCommandsParser.TimeFormatContext): Void? {
            super.visitTimeFormat(ctx)
            markKeyWords(ctx)
            return null
        }

        private fun markKeyWords(ctx: ParseTree) {
            if (ctx is TerminalNode) {
                val token = ctx.symbol
                when (token.type) {
                    EnglishCommandsParser.SINCE, EnglishCommandsParser.FROM, EnglishCommandsParser.AGO, EnglishCommandsParser.AT, EnglishCommandsParser.TO, EnglishCommandsParser.UNTIL, EnglishCommandsParser.FIN, EnglishCommandsParser.RESUME, EnglishCommandsParser.LAST -> addHighlight(token, token, "keyword")
                    EnglishCommandsParser.DAYS, EnglishCommandsParser.HOURS, EnglishCommandsParser.MINUTES, EnglishCommandsParser.SECONDS -> addHighlight(token, token, "timeUnit")
                    EnglishCommandsParser.NUMBER, EnglishCommandsParser.COLON -> addHighlight(token, token, "value")
                    else -> Unit
                }
            }
            if (ctx is EnglishCommandsParser.DateTimeContext) {
                addHighlight(ctx.start, ctx.stop, "dateTime")
            } else {
                for (i in 0 until ctx.childCount) {
                    markKeyWords(ctx.getChild(i))
                }
            }
        }

        private fun addHighlight(start: Token, stop: Token, style: String) {
            textArea.setStyle(start.startIndex, stop.stopIndex + 1, setOf(style))
        }
    }

    class Factory @Inject
    constructor(private val itemGrouper: @JvmSuppressWildcards ItemGrouper, private val config: ReportConfig) {

        fun create(textArea: StyleClassedTextArea): CommandHighlighter {
            return CommandHighlighter(itemGrouper, config, textArea)
        }
    }
}
