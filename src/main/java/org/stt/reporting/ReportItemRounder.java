package org.stt.reporting;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.Duration;
import org.stt.model.ReportingItem;
import org.stt.time.DurationRounder;

/**
 *
 * @author dante
 */
public class ReportItemRounder {

	private final DurationRounder durationRounder;

	public ReportItemRounder(DurationRounder durationRounder) {
		Preconditions.checkNotNull(durationRounder);
		this.durationRounder = durationRounder;
	}

	public Map<ReportingItem, Duration> mapFromReportItemToRoundedDuration(Collection<ReportingItem> reportingItems) {
		Preconditions.checkNotNull(reportingItems);
		Map<ReportingItem, Duration> result = new HashMap<>();
		for (ReportingItem reportingItem : reportingItems) {
			result.put(reportingItem, durationRounder.roundDuration(reportingItem.getDuration()));
		}
		return result;
	}

}
