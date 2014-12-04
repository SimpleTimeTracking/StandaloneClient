package org.stt.fun;

import org.stt.model.TimeTrackingItem;

import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Achieved, if enough distinct comments with more than a given threshold
 * characters are tracked.
 *
 * @author dante
 */
class LongComments extends LocalizedAchievement {

    private final int times;
    private Set<String> matches;
    private int threshold;
    private boolean achieved;

    public LongComments(ResourceBundle resourceBundle, int times, int threshold) {
        super(resourceBundle);
        this.times = times;
        this.threshold = threshold;
    }

    @Override
    void start() {
        matches = new HashSet<String>();
        achieved = false;
    }

    @Override
    void process(TimeTrackingItem read) {
        final String comment = read.getComment().or("");
        if (comment.length() >= threshold && !matches.contains(comment)) {
            matches.add(comment);
        }
    }

    @Override
    void done() {
        achieved = matches.size() >= times;
        matches = null;
    }

    @Override
    boolean isAchieved() {
        return achieved;
    }

    @Override
    public String getCode() {
        return "longComment" + times;
    }

    @Override
    public String getDescription() {
        return String.format(localize("achievement.longComments"), times, threshold);
    }

}
