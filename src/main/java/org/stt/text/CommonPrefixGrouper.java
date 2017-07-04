package org.stt.text;

import org.stt.IntRange;
import org.stt.StopWatch;
import org.stt.config.YamlConfigService;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Learns common prefixes and uses them to determine groups.
 * Note that items are split at 'space' unless the resulting subgroup would have less than
 * 3 characters, in which case the group gets expanded.
 */
@Singleton
class CommonPrefixGrouper implements ItemGrouper, ExpansionProvider {
    public static final int MINIMUM_GROUP_LENGTH = 3;
    private final TimeTrackingItemQueries queries;
    private final YamlConfigService yamlConfig;
    private boolean initialized;
    private PrefixTree root = new PrefixTree();

    @Inject
    public CommonPrefixGrouper(TimeTrackingItemQueries queries,
                               YamlConfigService yamlConfig) {
        this.queries = Objects.requireNonNull(queries);
        this.yamlConfig = Objects.requireNonNull(yamlConfig);
    }

    @Override
    public List<Group> getGroupsOf(String text) {
        Objects.requireNonNull(text);
        checkInitialized();
        ArrayList<Group> groups = new ArrayList<>();
        char[] chars = text.toCharArray();
        int n = chars.length;
        PrefixTree node = root;
        int i = 0;
        int start = 0;
        while (i < n && node != null) {
            char lastChar = (char) -1;
            while (i < n && node != null && (node.numChildren() <= 1 || i < start + 3)) {
                lastChar = chars[i];
                node = node.child(lastChar);
                i++;
            }
            while (i < n && node != null && lastChar != ' ') {
                lastChar = chars[i];
                node = node.child(lastChar);
                i++;
            }
            groups.add(new Group(Type.MATCH, text.substring(start, i), new IntRange(start, i)));
            start = i;
        }
        if (i < n) {
            groups.add(new Group(Type.REMAINDER, text.substring(i, n), new IntRange(i, n)));
        }
        return groups;
    }

    private void checkInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        StopWatch stopWatch = new StopWatch("Item grouper");
        queries.queryAllItems()
                .map(TimeTrackingItem::getActivity)
                .forEach(this::insert);

        yamlConfig.getConfig().getPrefixGrouper().getBaseLine()
                .forEach(this::insert);


        stopWatch.stop();
    }

    public void insert(String item) {
        PrefixTree node = root;
        int i = 0;
        int n = item.length();

        char[] chars = item.toCharArray();
        while (i < n) {
            PrefixTree child = node.child(chars[i]);
            if (child != null) {
                node = child;
                i++;
            } else {
                break;
            }
        }
        while (i < n) {
            PrefixTree newChild = new PrefixTree();
            node.child(chars[i], newChild);
            i++;
            node = newChild;
        }
        node.child(null, null);
    }


    @Override
    public List<String> getPossibleExpansions(String text) {
        Objects.requireNonNull(text);
        checkInitialized();

        char[] chars = text.toCharArray();
        PrefixTree node = root;
        int i = 0;
        int n = chars.length;
        while (i < n && node != null) {
            node = node.child(chars[i]);
            i++;
        }
        return node == null ? Collections.emptyList() :
                node.allChildren().stream()
                        .map(entry -> {
                            PrefixTree tree = entry.getValue();
                            StringBuilder current = new StringBuilder();
                            current.append(entry.getKey());
                            while (tree != null && tree.numChildren() == 1) {
                                Character childChar = tree.anyChild();
                                tree = tree.child(childChar);
                                if (childChar != null) {
                                    current.append(childChar);
                                }
                            }
                            return current.toString();
                        })
                        .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "";
    }

    private static class PrefixTree {
        private Map<Character, PrefixTree> child;

        protected PrefixTree child(Character c) {
            if (child == null) {
                child = new HashMap<>();
            }
            return child.get(c);
        }

        public void child(Character c, PrefixTree newChild) {
            if (child == null) {
                child = new HashMap<>();
            }
            child.put(c, newChild);
        }

        public int numChildren() {
            return child == null ? 0 : child.size();
        }

        public Character anyChild() {
            return child.keySet().iterator().next();
        }

        public Set<Map.Entry<Character, PrefixTree>> allChildren() {
            return child == null ? Collections.emptySet() : child.entrySet();
        }
    }
}
