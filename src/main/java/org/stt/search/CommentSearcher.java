package org.stt.search;

import java.util.Collection;

public interface CommentSearcher {
	/**
	 * Given a partially entered comment, finds best matching existing comments.
	 * <p>
	 * No guarantees are given for the amount of hits returned, zero is
	 * perfectly valid - even if there are hits.
	 * </p>
	 * <p>
	 * The returned comments are in order of relevancy (depending on context,
	 * i.e. most recent to least recent matching)
	 * </p>
	 * 
	 * @param query
	 * @return
	 */
	Collection<String> searchForComments(String partialComment);
}
