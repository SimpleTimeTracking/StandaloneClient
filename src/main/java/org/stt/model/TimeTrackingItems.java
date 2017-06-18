package org.stt.model;

/**
 * "Companion" object for {@link TimeTrackingItem}.
 */
public final class TimeTrackingItems {
    private TimeTrackingItems() {
    }

    public static boolean sameStart(TimeTrackingItem a, TimeTrackingItem b) {
        return a.getStart().equals(b.getStart());
    }

    public static boolean sameActivity(TimeTrackingItem a, TimeTrackingItem b) {
        return a.getActivity().equals(b.getActivity());
    }

    public static boolean sameEnd(TimeTrackingItem a, TimeTrackingItem b) {
        return a.getEnd()
                .map(endA -> b.getEnd().map(endA::equals).orElse(false))
                .orElse(!b.getEnd().isPresent());
    }
}
