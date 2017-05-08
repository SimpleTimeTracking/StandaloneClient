package org.stt.query;

import org.stt.reporting.WorkingtimeItemProvider;
import org.stt.text.ItemCategorizer;
import org.stt.time.DateTimes;
import org.stt.time.Interval;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Singleton
public class WorkTimeQueries {
    private WorkingtimeItemProvider workingtimeItemProvider;
    private ItemCategorizer itemCategorizer;
    private TimeTrackingItemQueries timeTrackingItemQueries;

    @Inject
    public WorkTimeQueries(WorkingtimeItemProvider workingtimeItemProvider, ItemCategorizer itemCategorizer,
                           TimeTrackingItemQueries timeTrackingItemQueries) {
        this.workingtimeItemProvider = Objects.requireNonNull(workingtimeItemProvider);
        this.itemCategorizer = Objects.requireNonNull(itemCategorizer);
        this.timeTrackingItemQueries = Objects.requireNonNull(timeTrackingItemQueries);
    }

    public Duration queryRemainingWorktimeToday() {
        LocalDateTime now = DateTimes.preciseToSecond(LocalDateTime.now());
        LocalDate today = now.toLocalDate();
        Duration workedTime = queryWorktime(Interval.ofDay(today).withEnd(now));
        return workingtimeItemProvider.getWorkingTimeFor(today).getMin().minus(workedTime);
    }

    public Duration queryWeekWorktime() {
        LocalDateTime now = DateTimes.preciseToSecond(LocalDateTime.now());
        LocalDate monday = now.toLocalDate().with(DayOfWeek.MONDAY);
        Interval currentWeek = Interval.between(monday.atStartOfDay(), now);
        return queryWorktime(currentWeek);
    }

    public Duration queryWorktime(Interval interval) {
        Criteria criteria = new Criteria();
        criteria.withStartNotBefore(interval.getStart());
        criteria.withStartBefore(interval.getEnd());
        return timeTrackingItemQueries.queryItems(criteria)
                .filter(item -> itemCategorizer.getCategory(item.getActivity()) == ItemCategorizer.ItemCategory.WORKTIME)
                .map(item -> {
                    LocalDateTime end = item.getEnd().orElse(interval.getEnd());
                    return Duration.between(item.getStart(), end);
                })
                .reduce(Duration.ZERO, Duration::plus);
    }
}
