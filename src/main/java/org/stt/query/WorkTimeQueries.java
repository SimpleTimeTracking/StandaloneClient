package org.stt.query;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.stt.analysis.ItemCategorizer;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.query.Query;
import org.stt.reporting.WorkingtimeItemProvider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 29.03.15.
 */
public class WorkTimeQueries {

    private WorkingtimeItemProvider workingtimeItemProvider;
    private ItemCategorizer itemCategorizer;
    private TimeTrackingItemQueries timeTrackingItemQueries;

    @Inject
    public WorkTimeQueries(WorkingtimeItemProvider workingtimeItemProvider, ItemCategorizer itemCategorizer,
                           TimeTrackingItemQueries timeTrackingItemQueries) {
        this.workingtimeItemProvider = checkNotNull(workingtimeItemProvider);
        this.itemCategorizer = checkNotNull(itemCategorizer);
        this.timeTrackingItemQueries = checkNotNull(timeTrackingItemQueries);
    }

    public Duration queryRemainingWorktimeToday() {
        Duration workedTime = Duration.ZERO;
        DateTime now = new DateTime();
        LocalDate today = new LocalDate(now);
        Query query = new Query();
        query.withPeriodAtDay(today);
        for (TimeTrackingItem item: timeTrackingItemQueries.queryItems(query)) {
            if (itemCategorizer.getCategory(item.getComment().or("")) == ItemCategorizer.ItemCategory.WORKTIME) {
                DateTime end = item.getEnd().or(now);
                workedTime = workedTime.plus(new Duration(item.getStart(), end));
            }
        }

        Duration remainingDuration = workingtimeItemProvider.getWorkingTimeFor(today).getMin().minus(workedTime);
        if (remainingDuration.isShorterThan(Duration.ZERO)) {
            remainingDuration = Duration.ZERO;
        }
        return remainingDuration;
    }
}
