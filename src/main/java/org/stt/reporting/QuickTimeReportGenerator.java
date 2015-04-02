package org.stt.reporting;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.stt.analysis.ItemCategorizer;
import org.stt.model.TimeTrackingItem;
import org.stt.search.ItemSearcher;
import org.stt.search.Query;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 29.03.15.
 */
public class QuickTimeReportGenerator {

    private WorkingtimeItemProvider workingtimeItemProvider;
    private ItemCategorizer itemCategorizer;
    private ItemSearcher itemSearcher;

    @Inject
    public QuickTimeReportGenerator(WorkingtimeItemProvider workingtimeItemProvider, ItemCategorizer itemCategorizer,
                                    ItemSearcher itemSearcher) {
        this.workingtimeItemProvider = checkNotNull(workingtimeItemProvider);
        this.itemCategorizer = checkNotNull(itemCategorizer);
        this.itemSearcher = checkNotNull(itemSearcher);
    }

    public QuickTimeReport queryReport() {
        QuickTimeReport quickTimeReport = new QuickTimeReport();
        Duration workedTime = Duration.ZERO;
        DateTime now = new DateTime();
        LocalDate today = new LocalDate(now);
        Query query = new Query();
        query.withPeriodAtDay(today);
        for (TimeTrackingItem item: itemSearcher.queryItems(query)) {
            if (itemCategorizer.getCategory(item.getComment().or("")) == ItemCategorizer.ItemCategory.BREAK) {
                DateTime end = item.getEnd().or(now);
                workedTime = workedTime.plus(new Duration(item.getStart(), end));
            }
        }

        Duration remainingDuration = workingtimeItemProvider.getWorkingTimeFor(today).getMin().minus(workedTime);
        if (remainingDuration.isShorterThan(Duration.ZERO)) {
            remainingDuration = Duration.ZERO;
        }
        quickTimeReport.remainingDuration = remainingDuration;
        return quickTimeReport;
    }

    public static class QuickTimeReport {
        Duration remainingDuration = Duration.ZERO;


        public Duration getRemainingDuration() {
            return remainingDuration;
        }
    }
}
