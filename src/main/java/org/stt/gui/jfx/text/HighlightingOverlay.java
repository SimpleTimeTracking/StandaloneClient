package org.stt.gui.jfx.text;

import com.sun.javafx.scene.control.skin.TextAreaSkin;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 06.12.14.
 */
public class HighlightingOverlay {
    private static final Logger LOG = Logger.getLogger(HighlightingOverlay.class
            .getName());

    private Region region;
    private ObservableList<Node> children;
    private TextArea target;
    private TextAreaSkin textAreaSkin;
    private List<Highlight> highlights = new ArrayList<>();

    public HighlightingOverlay(TextArea target) {
        this.target = checkNotNull(target);
        updateInternalAccess();
    }

    @SuppressWarnings("unchecked")
    private boolean updateInternalAccess() {
        textAreaSkin = (TextAreaSkin) target.getSkin();
        if (textAreaSkin == null) {
            return false;
        }
        try {
            Field contentView = TextAreaSkin.class.getDeclaredField("contentView");
            contentView.setAccessible(true);
            region = (Region) contentView.get(target.getSkin());
            contentView.setAccessible(false);

            Method getChildren = Parent.class.getDeclaredMethod("getChildren");
            getChildren.setAccessible(true);
            children = (ObservableList<Node>) getChildren.invoke(region);
            getChildren.setAccessible(false);
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to apply text highlighting", e);
            return false;
        }
    }

    public void addHighlight(Highlight highlight) {
        checkNotNull(highlight);
        if (region == null && !updateInternalAccess()) {
            return;
        }

        highlight.layoutUsing(target, region, textAreaSkin);
        children.addAll(highlight.rectangle);
        highlights.add(highlight);
    }

    public void clearHighlights() {
        if (children == null) {
            return;
        }
        for (Highlight h: highlights) {
            children.removeAll(h.rectangle);
        }
        highlights.clear();
    }

    public static class Highlight {
        protected Rectangle rectangle[];
        private int start;

        public Highlight(int from, int to, Color color) {
            this.start = from;
            rectangle = new Rectangle[to - from + 1];
            for (int i = 0; i < rectangle.length; i++) {
                Rectangle rec = new Rectangle();
                rec.setDisable(true);
                rec.setBlendMode(BlendMode.SCREEN);
                rec.setFill(color);
                rectangle[i] = rec;
            }
        }

        protected void layoutUsing(TextArea target, Region within, TextAreaSkin skin) {
            int pos = start;
            for (Rectangle rec : rectangle) {
                Rectangle2D characterBounds = skin.getCharacterBounds(pos);
                Point2D point2D = target.localToScene(characterBounds.getMinX(), characterBounds.getMinY());
                point2D = within.sceneToLocal(point2D);
                rec.setX(point2D.getX());
                rec.setY(point2D.getY());
                rec.setWidth(characterBounds.getWidth());
                rec.setHeight(characterBounds.getHeight());

                pos++;
            }
        }
    }
}
