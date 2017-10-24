package org.stt.gui.jfx.text;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.stt.command.CaseInsensitiveInputStream;
import org.stt.config.ReportConfig;
import org.stt.grammar.EnglishCommandsBaseVisitor;
import org.stt.grammar.EnglishCommandsLexer;
import org.stt.grammar.EnglishCommandsParser;
import org.stt.grammar.EnglishCommandsVisitor;
import org.stt.text.ItemGrouper;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class CommandHighlighter {
    private final ItemGrouper itemGrouper;
    private final ReportConfig config;
    private StyleClassedTextArea textArea;
    private EnglishCommandsVisitor<Void> visitor = new Highlighter();

    public CommandHighlighter(ItemGrouper itemGrouper,
                              ReportConfig config,
                              StyleClassedTextArea styleClassedTextArea) {
        this.itemGrouper = itemGrouper;
        this.config = config;
        this.textArea = Objects.requireNonNull(styleClassedTextArea);
    }

    public void update() {
        if (textArea.getLength() == 0) {
            return;
        }
        textArea.clearStyle(0, textArea.getLength());
        String text = textArea.getText();
        CharStream input = new CaseInsensitiveInputStream(text);
        EnglishCommandsLexer lexer = new EnglishCommandsLexer(input);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        EnglishCommandsParser parser = new EnglishCommandsParser(tokenStream);
        parser.command().accept(visitor);
        ItemGrouper.Group[] groups = itemGrouper.getGroupsOf(text).toArray(new ItemGrouper.Group[0]);
        for (int i = 0; i < groups.length - 1; i++) {
            ItemGrouper.Group group = groups[i];
            if (group.type == ItemGrouper.Type.MATCH) {
                textArea.setStyle(group.range.start, group.range.end, Arrays.asList("matchedGroup", "group" + i));
            }
        }
        textArea.setStyle("-fx-fill: red;");
    }

    private class Highlighter extends EnglishCommandsBaseVisitor<Void> {
        @Override
        public Void visitDateTime(EnglishCommandsParser.DateTimeContext ctx) {
            markKeyWords(ctx);
            return null;
        }

        @Override
        public Void visitTimeFormat(EnglishCommandsParser.TimeFormatContext ctx) {
            super.visitTimeFormat(ctx);
            markKeyWords(ctx);
            return null;
        }

        private void markKeyWords(ParseTree ctx) {
            if (ctx instanceof TerminalNode) {
                Token token = ((TerminalNode) ctx).getSymbol();
                switch (token.getType()) {
                    case EnglishCommandsParser.SINCE:
                    case EnglishCommandsParser.FROM:
                    case EnglishCommandsParser.AGO:
                    case EnglishCommandsParser.AT:
                    case EnglishCommandsParser.TO:
                    case EnglishCommandsParser.UNTIL:
                    case EnglishCommandsParser.FIN:
                    case EnglishCommandsParser.RESUME:
                    case EnglishCommandsParser.LAST:
                        addHighlight(token, token, "keyword");
                        break;
                    case EnglishCommandsParser.DAYS:
                    case EnglishCommandsParser.HOURS:
                    case EnglishCommandsParser.MINUTES:
                    case EnglishCommandsParser.SECONDS:
                        addHighlight(token, token, "timeUnit");
                        break;
                    case EnglishCommandsParser.NUMBER:
                    case EnglishCommandsParser.COLON:
                        addHighlight(token, token, "value");
                        break;
                    default:
                        break;
                }
            }
            if (ctx instanceof EnglishCommandsParser.DateTimeContext) {
                EnglishCommandsParser.DateTimeContext dateTimeContext = (EnglishCommandsParser.DateTimeContext) ctx;
                addHighlight(dateTimeContext.start, dateTimeContext.stop, "dateTime");
            } else {
                for (int i = 0; i < ctx.getChildCount(); i++) {
                    markKeyWords(ctx.getChild(i));
                }
            }
        }

        private void addHighlight(Token start, Token stop, String style) {
            textArea.setStyle(start.getStartIndex(), stop.getStopIndex() + 1, Collections.singleton(style));
        }
    }

    public static class Factory {
        private final ItemGrouper itemGrouper;
        private final ReportConfig config;

        @Inject
        public Factory(ItemGrouper itemGrouper, ReportConfig config) {
            this.itemGrouper = itemGrouper;
            this.config = config;
        }

        public CommandHighlighter create(StyleClassedTextArea textArea) {
            return new CommandHighlighter(itemGrouper, config, textArea);
        }
    }
}
