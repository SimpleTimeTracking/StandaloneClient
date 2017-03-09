package org.stt;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class Streams {
    private Streams() {
    }

    public static <T> Predicate<T> distinctByKey(Function<T, Object> keyExtractor) {
        Set<Object> visited = new HashSet<>();
        return item -> visited.add(keyExtractor.apply(item));
    }
}
