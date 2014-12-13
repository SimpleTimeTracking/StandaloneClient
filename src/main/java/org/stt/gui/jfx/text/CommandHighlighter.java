package org.stt.gui.jfx.text;

import javafx.scene.paint.Color;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.stt.g4.EnglishCommandsBaseVisitor;
import org.stt.g4.EnglishCommandsParser;
import org.stt.g4.EnglishCommandsVisitor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 06.12.14.
 */
public class CommandHighlighter {
    private HighlightingOverlay overlay;
    private EnglishCommandsVisitor<Void> visitor = new Highlighter();

    public CommandHighlighter(HighlightingOverlay overlay) {
        this.overlay = checkNotNull(overlay);
    }

    public void addHighlights(EnglishCommandsParser.CommandContext context) {
        context.accept(visitor);
    }

    private class Highlighter extends EnglishCommandsBaseVisitor<Void> {
        @Override
        public Void visitDateTime(@NotNull EnglishCommandsParser.DateTimeContext ctx) {
            markKeyWords(ctx);
            return null;
        }

        @Override
        public Void visitTimeFormat(@NotNull EnglishCommandsParser.TimeFormatContext ctx) {
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
                        addHighlight(token, token, Color.BLUE);
                        break;
                    case EnglishCommandsParser.DAYS:
                    case EnglishCommandsParser.HOURS:
                    case EnglishCommandsParser.MINUTES:
                    case EnglishCommandsParser.SECONDS:
                        addHighlight(token, token, Color.BROWN);
                        break;
                    case EnglishCommandsParser.NUMBER:
                    case EnglishCommandsParser.COLON:
                        addHighlight(token, token, Color.DARKGREEN);
                        break;
                    default:
                        break;
                }
            }
            for (int i = 0; i < ctx.getChildCount(); i++) {
                markKeyWords(ctx.getChild(i));
            }
        }

        private void addHighlight(Token start, Token stop, Color color) {
            HighlightingOverlay.Highlight highlight = new HighlightingOverlay.Highlight(start.getStartIndex(), stop.getStopIndex(), color);
            overlay.addHighlight(highlight);
        }
    }
}
