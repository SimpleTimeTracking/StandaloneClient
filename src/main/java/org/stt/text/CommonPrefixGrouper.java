package org.stt.text;

import org.stt.IntRange;
import org.stt.model.TimeTrackingItem;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Stream;

/**
 * Learns common prefixes and uses them to determine groups.
 * Note that items are split at 'space' unless the resulting subgroup would have less than
 * 3 characters, in which case the group gets expanded.
 */
@Singleton
class CommonPrefixGrouper implements ItemGrouper, ExpansionProvider {
    public static final int MINIMUM_GROUP_LENGTH = 3;
    private final RadixTreeNode root = new RadixTreeNode();

    @Inject
    public CommonPrefixGrouper() {
        // Required by Dagger
    }

    @Override
    public List<Group> getGroupsOf(String text) {
        Objects.requireNonNull(text);
        return root.findGroupsFor(text);
    }

    @Override
    public List<String> getPossibleExpansions(String text) {
        Objects.requireNonNull(text);
        return root.findExpansions(text);
    }

    public void scanForGroups(Stream<TimeTrackingItem> items) {
        Objects.requireNonNull(items);

        items.map(TimeTrackingItem::getActivity)
                .forEach(this::learnLine);
    }

    public void learnLine(String comment) {
        root.insert(comment);
    }

    @Override
    public String toString() {
        return root.toString();
    }

    private static class RadixTreeNode {
        private final Set<RadixTreeNode> children = new HashSet<>();
        private String prefix = "";

        public void insert(String comment) {
            if (comment.isEmpty()) {
                return;
            }
            Match bestMatch = findChildWithLongestCommonPrefix(comment);
            if (bestMatch != null) {
                RadixTreeNode groupForRemaining = bestMatch.group;
                if (!bestMatch.isMatchingCompletePrefix()) {
                    groupForRemaining = splitChildAt(bestMatch.group,
                            bestMatch.prefixLength);
                }
                if (comment.length() > bestMatch.prefixLength) {
                    String remainingComment = comment
                            .substring(bestMatch.prefixLength + 1);
                    groupForRemaining.insert(remainingComment);
                }
            } else {
                addChildWithPrefix(comment);
            }
        }

        public List<Group> findGroupsFor(String comment) {
            return addGroupOfCommentTo(comment, new ArrayList<>(), 0);
        }

        private List<Group> addGroupOfCommentTo(String comment,
                                                ArrayList<Group> result, int position) {
            Match match = comment.length() >= MINIMUM_GROUP_LENGTH ? findChildWithLongestCommonPrefix(comment) : null;
            if (match != null && match.isMatchingCompletePrefix()
                    && comment.length() >= match.prefixLength) {
                result.add(new Group(Type.MATCH, match.group.prefix, new IntRange(position, position + match.prefixLength)));
                if (comment.length() > match.prefixLength) {
                    match.group.addGroupOfCommentTo(
                            comment.substring(match.prefixLength + 1), result, position + match.prefixLength + 1);
                }
            } else {
                result.add(new Group(Type.REMAINDER, comment, new IntRange(position, position + comment.length())));
            }
            return result;
        }

        private RadixTreeNode splitChildAt(RadixTreeNode group, int position) {
            String newChildPrefix = group.prefix.substring(0, position);
            RadixTreeNode newChild = addChildWithPrefix(newChildPrefix);

            group.prefix = group.prefix.substring(position + 1);
            moveChildTo(group, newChild);
            return newChild;
        }

        private void moveChildTo(RadixTreeNode group, RadixTreeNode newParent) {
            children.remove(group);
            newParent.children.add(group);
        }

        private RadixTreeNode addChildWithPrefix(String newChildPrefix) {
            RadixTreeNode newChild = new RadixTreeNode();
            newChild.prefix = newChildPrefix;
            children.add(newChild);
            return newChild;
        }

        public List<String> findExpansions(String comment) {
            List<String> result = new ArrayList<>();
            addExpansionsTo(result, comment);
            return result;
        }

        private void addExpansionsTo(List<String> result, String comment) {
            int matchLength = lengthOfCommonPrefix(comment);
            if (matchLength == prefix.length()
                    && matchLength <= comment.length()) {
                String remaining = comment.substring(matchLength).trim();
                for (RadixTreeNode child : children) {
                    child.addExpansionsTo(result, remaining);
                }
            } else if (matchLength == comment.length()
                    && matchLength < prefix.length()) {
                result.add(prefix.substring(matchLength));
            }
        }

        private Match findChildWithLongestCommonPrefix(String comment) {
            Match match = new Match();
            for (RadixTreeNode grp : children) {
                int currentLength = grp
                        .lengthOfValidLongestCommonPrefix(comment);
                if (currentLength > match.prefixLength) {
                    match.prefixLength = currentLength;
                    match.group = grp;
                }
            }
            return match.prefixLength == 0 ? null : match;
        }

        private int lengthOfValidLongestCommonPrefix(String text) {
            int currentLength = lengthOfLongestCommonPrefix(text);
            if (currentLength >= MINIMUM_GROUP_LENGTH) {
                return currentLength;
            }
            return 0;
        }

        private int lengthOfLongestCommonPrefix(String text) {
            int lastDelimitedPrefix = 0;
            int i = 0;
            for (; i < prefix.length() && i < text.length(); i++) {
                if (text.charAt(i) != prefix.charAt(i)) {
                    return lastDelimitedPrefix;
                }
                if (text.charAt(i) == ' ') {
                    lastDelimitedPrefix = i;
                }
            }
            if (i == text.length() || text.charAt(i) == ' ') {
                lastDelimitedPrefix = i;
            }
            return lastDelimitedPrefix;
        }

        private int lengthOfCommonPrefix(String other) {
            int i = 0;
            for (; i < other.length() && i < prefix.length(); i++) {
                if (other.charAt(i) != prefix.charAt(i)) {
                    return i;
                }
            }
            return i;
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            for (RadixTreeNode child : children) {
                out.append('\'').append(child.prefix).append("' = [");
                out.append(child.toString());
                out.append(']');
            }
            return out.toString();
        }
    }

    private static class Match {
        protected RadixTreeNode group;
        protected int prefixLength;

        protected boolean isMatchingCompletePrefix() {
            return group.prefix.length() == prefixLength;
        }
    }
}
