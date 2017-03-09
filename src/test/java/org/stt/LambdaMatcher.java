package org.stt;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.function.Function;

public class LambdaMatcher {
    private LambdaMatcher() {
    }

    public static <T, R> Matcher<T> mapped(Function<T, R> mapper, Matcher<R> mappedMatcher) {
        return new BaseMatcher<T>() {
            @Override
            public boolean matches(Object item) {
                return mappedMatcher.matches(mapper.apply((T) item));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(" mapped ");
                mappedMatcher.describeTo(description);
            }
        };
    }
}
