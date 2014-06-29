package org.stt.reporting;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class CommonPrefixGrouper implements ItemGrouper {
	private final Group root = new Group();

	@Override
	public List<String> getGroupsOf(TimeTrackingItem item) {
		checkNotNull(item);
		if (!item.getComment().isPresent()) {
			return Collections.emptyList();
		}

		return root.findGroupsFor(item.getComment().get());
	}

	public void scanForGroups(ItemReader itemReader) {
		checkNotNull(itemReader);

		Optional<TimeTrackingItem> item;
		while ((item = itemReader.read()).isPresent()) {
			Optional<String> optComment = item.get().getComment();
			if (optComment.isPresent()) {
				String comment = optComment.get();
				root.insert(comment);
			}
		}
	}

	@Override
	public String toString() {
		return root.toString();
	}

	private static class Group {
		private final Set<Group> children = new HashSet<CommonPrefixGrouper.Group>();
		private String prefix = "";

		public void insert(String comment) {
			if (comment.isEmpty()) {
				return;
			}
			Match bestMatch = findMatch(comment);
			if (bestMatch != null) {
				Group groupForRemaining = bestMatch.group;
				if (!bestMatch.isMatchingCompletePrefix()) {
					groupForRemaining = splitChildAt(bestMatch.group,
							bestMatch.prefixLength);
				}
				if (comment.length() > bestMatch.prefixLength) {
					String remainingComment = comment
							.substring(bestMatch.prefixLength + 1);
					groupForRemaining.insert(remainingComment);
				}
			} else {
				addChildWithPrefix(comment);
			}
		}

		public List<String> findGroupsFor(String comment) {
			return addGroupOfCommentTo(comment, new ArrayList<String>());
		}

		private List<String> addGroupOfCommentTo(String comment,
				ArrayList<String> result) {
			Match match = findMatch(comment);
			if (match != null && match.isMatchingCompletePrefix()
					&& comment.length() > match.prefixLength) {
				result.add(match.group.prefix);
				match.group.addGroupOfCommentTo(
						comment.substring(match.prefixLength + 1), result);
			} else {
				result.add(comment);
			}
			return result;
		}

		private Group splitChildAt(Group group, int position) {
			String newChildPrefix = group.prefix.substring(0, position);
			Group newChild = addChildWithPrefix(newChildPrefix);

			group.prefix = group.prefix.substring(position + 1);
			moveChildTo(group, newChild);
			return newChild;
		}

		private void moveChildTo(Group group, Group newParent) {
			children.remove(group);
			newParent.children.add(group);
		}

		private Group addChildWithPrefix(String newChildPrefix) {
			Group newChild = new Group();
			newChild.prefix = newChildPrefix;
			children.add(newChild);
			return newChild;
		}

		private Match findMatch(String comment) {
			Match match = new Match();
			for (Group grp : children) {
				int currentLength = grp.matchWith(comment);
				if (currentLength > match.prefixLength) {
					match.prefixLength = currentLength;
					match.group = grp;
				}
			}
			return match.prefixLength == 0 ? null : match;
		}

		private int matchWith(String text) {
			int currentLength = 0;
			for (int i = 0; i < prefix.length() && i < text.length(); i++) {
				if (text.charAt(i) != prefix.charAt(i)) {
					break;
				}
				if (text.charAt(i) == ' ') {
					currentLength = i;
				} else if (i == text.length() - 1 || i == prefix.length() - 1
						&& text.charAt(i + 1) == ' ') {
					currentLength = i + 1;
				}
			}
			if (currentLength > 3) {
				return currentLength;
			}
			return 0;
		}

		@Override
		public String toString() {
			StringBuilder out = new StringBuilder();
			for (Group child : children) {
				out.append('\'').append(child.prefix).append("' = [");
				out.append(child.toString());
				out.append(']');
			}
			return out.toString();
		}
	}

	private static class Match {
		protected Group group;
		protected int prefixLength;

		protected boolean isMatchingCompletePrefix() {
			return group.prefix.length() == prefixLength;
		}
	}
}
