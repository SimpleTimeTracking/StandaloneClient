package org.stt.model;

import org.joda.time.Duration;

public class ReportingItem {

	private final Duration duration;
	private final String comment;

	public ReportingItem(Duration duration, String comment) {
		this.duration = duration;
		this.comment = comment;
	}

	public Duration getDuration() {
		return duration;
	}

	public String getComment() {
		return comment;
	}

	public ReportingItem addDurationOf(ReportingItem item) {
		return new ReportingItem(getDuration().plus(duration), comment);
	}

	@Override
	public String toString() {
		return duration.toString() + " " + comment;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result
				+ ((duration == null) ? 0 : duration.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ReportingItem other = (ReportingItem) obj;
		if (comment == null) {
			if (other.comment != null) {
				return false;
			}
		} else if (!comment.equals(other.comment)) {
			return false;
		}
		if (duration == null) {
			if (other.duration != null) {
				return false;
			}
		} else if (!duration.equals(other.duration)) {
			return false;
		}
		return true;
	}
}
