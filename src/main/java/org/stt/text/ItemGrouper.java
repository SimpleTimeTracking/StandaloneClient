package org.stt.text;

import org.stt.IntRange;

import java.util.List;

public interface ItemGrouper {
    List<Group> getGroupsOf(String text);

    class Group {
        public final Type type;
        public final String content;
        public final IntRange range;

        public Group(Type type, String content, IntRange range) {
            this.type = type;
            this.content = content;
            this.range = range;
        }
    }

    enum Type {
        MATCH, REMAINDER;
    }
}
