package org.stt.gui.jfx.text;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.stt.g4.EnglishCommandsBaseVisitor;
import org.stt.g4.EnglishCommandsLexer;
import org.stt.g4.EnglishCommandsParser;
import org.stt.g4.EnglishCommandsVisitor;

import java.util.Collections;
import java.util.Objects;

public class CommandHighlighter {
    private StyleClassedTextArea textArea;
    private EnglishCommandsVisitor<Void> visitor = new Highlighter();

    public CommandHighlighter(StyleClassedTextArea styleClassedTextArea) {
        this.textArea = Objects.requireNonNull(styleClassedTextArea);
    }

    public void update() {
        CharStream input = new ANTLRInputStream(textArea.getText());
        EnglishCommandsLexer lexer = new EnglishCommandsLexer(input);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        EnglishCommandsParser parser = new EnglishCommandsParser(tokenStream);
        textArea.clearStyle(0, textArea.getLength());
        parser.command().accept(visitor);
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
            for (int i = 0; i < ctx.getChildCount(); i++) {
                markKeyWords(ctx.getChild(i));
            }
        }

        private void addHighlight(Token start, Token stop, String style) {
            textArea.setStyle(start.getStartIndex(), stop.getStopIndex() + 1, Collections.singletonList(style));
        }
    }
}
