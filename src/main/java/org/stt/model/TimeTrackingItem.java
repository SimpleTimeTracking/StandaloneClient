package org.stt.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.joda.time.DateTime;

import com.google.common.base.Optional;

public final class TimeTrackingItem {

	private final Optional<String> comment;
	private final DateTime start;
	private final Optional<DateTime> end;

	/**
	 * @param comment
	 *            comment string describing this item. May be null
	 * @param start
	 *            start time of the item
	 * @param end
	 *            end time of the item.
	 */
	public TimeTrackingItem(String comment, DateTime start, DateTime end) {
		this.comment = Optional.fromNullable(comment);
		this.start = checkNotNull(start);
		this.end = Optional.of(end);
		checkState(!end.isBefore(start),
				"end must not be before start for item " + this.toString());
	}

	/**
	 * @param comment
	 *            comment string describing this item. May be null
	 * @param start
	 *            start time of the item
	 */
	public TimeTrackingItem(String comment, DateTime start) {
		this.comment = Optional.fromNullable(comment);
		this.start = start;
		this.end = Optional.absent();
	}

	public Optional<String> getComment() {
		return comment;
	}

	public DateTime getStart() {
		return start;
	}

	public Optional<DateTime> getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return start.toString() + " - "
				+ (end.isPresent() ? end.get().toString() : "null") + " : "
				+ comment.orNull();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
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
		TimeTrackingItem other = (TimeTrackingItem) obj;
		if (comment == null) {
			if (other.comment != null) {
				return false;
			}
		} else if (!comment.equals(other.comment)) {
			return false;
		}
		if (end == null) {
			if (other.end != null) {
				return false;
			}
		} else if (!end.equals(other.end)) {
			return false;
		}
		if (start == null) {
			if (other.start != null) {
				return false;
			}
		} else if (!start.equals(other.start)) {
			return false;
		}
		return true;
	}

	public TimeTrackingItem withEnd(DateTime newEnd) {
		checkNotNull(newEnd);
		return new TimeTrackingItem(comment.orNull(), start, newEnd);
	}
}
