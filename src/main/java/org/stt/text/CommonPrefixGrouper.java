package org.stt.text;

import com.google.common.base.Optional;
import com.google.inject.Singleton;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Learns common prefixes and uses them to determine groups.
 * Note that items are split at 'space' unless the resulting subgroup would have less than
 * 3 characters, in which case the group gets expanded.
 */
@Singleton
class CommonPrefixGrouper implements ItemGrouper, ExpansionProvider {
	private final RadixTreeNode root = new RadixTreeNode();

	@Override
	public List<String> getGroupsOf(String text) {
		checkNotNull(text);
		return root.findGroupsFor(text);
	}

	@Override
	public List<String> getPossibleExpansions(String text) {
		checkNotNull(text);
		return root.findExpansions(text);
	}

	public void scanForGroups(ItemReader itemReader) {
		checkNotNull(itemReader);

		Optional<TimeTrackingItem> item;
		while ((item = itemReader.read()).isPresent()) {
			Optional<String> optComment = item.get().getComment();
			if (optComment.isPresent()) {
				String comment = optComment.get();
				learnLine(comment);
			}
		}
	}

	public void learnLine(String comment) {
		root.insert(comment);
	}

	@Override
	public String toString() {
		return root.toString();
	}

	private static class RadixTreeNode {
		private final Set<RadixTreeNode> children = new HashSet<>();
		private String prefix = "";

		public void insert(String comment) {
			if (comment.isEmpty()) {
				return;
			}
			Match bestMatch = findChildWithLongestCommonPrefix(comment);
			if (bestMatch != null) {
				RadixTreeNode groupForRemaining = bestMatch.group;
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
			Match match = findChildWithLongestCommonPrefix(comment);
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

		private RadixTreeNode splitChildAt(RadixTreeNode group, int position) {
			String newChildPrefix = group.prefix.substring(0, position);
			RadixTreeNode newChild = addChildWithPrefix(newChildPrefix);

			group.prefix = group.prefix.substring(position + 1);
			moveChildTo(group, newChild);
			return newChild;
		}

		private void moveChildTo(RadixTreeNode group, RadixTreeNode newParent) {
			children.remove(group);
			newParent.children.add(group);
		}

		private RadixTreeNode addChildWithPrefix(String newChildPrefix) {
			RadixTreeNode newChild = new RadixTreeNode();
			newChild.prefix = newChildPrefix;
			children.add(newChild);
			return newChild;
		}

		public List<String> findExpansions(String comment) {
			List<String> result = new ArrayList<>();
			addExpansionsTo(result, comment);
			return result;
		}

		private void addExpansionsTo(List<String> result, String comment) {
			int matchLength = lengthOfCommonPrefix(comment);
			// System.out.println(prefix + " vs " + comment + " " + matchLength
			// + " " + prefix.length() + " " + comment.length());
			if (matchLength == prefix.length()
					&& matchLength <= comment.length()) {
				String remaining = comment.substring(matchLength).trim();
				for (RadixTreeNode child : children) {
					child.addExpansionsTo(result, remaining);
				}
			} else if (matchLength == comment.length()
					&& matchLength < prefix.length()) {
				result.add(prefix.substring(matchLength));
			}
		}

		private Match findChildWithLongestCommonPrefix(String comment) {
			Match match = new Match();
			for (RadixTreeNode grp : children) {
				int currentLength = grp
						.lengthOfValidLongestCommonPrefix(comment);
				if (currentLength > match.prefixLength) {
					match.prefixLength = currentLength;
					match.group = grp;
				}
			}
			return match.prefixLength == 0 ? null : match;
		}

		private int lengthOfValidLongestCommonPrefix(String text) {
			int currentLength = lengthOfLongestCommonPrefix(text);
			if (currentLength >= 3) {
				return currentLength;
			}
			return 0;
		}

		private int lengthOfLongestCommonPrefix(String text) {
			int lastDelimitedPrefix = 0;
			int i = 0;
			for (; i < prefix.length() && i < text.length(); i++) {
				if (text.charAt(i) != prefix.charAt(i)) {
					return lastDelimitedPrefix;
				}
				if (text.charAt(i) == ' ') {
					lastDelimitedPrefix = i;
				}
			}
			if (i == text.length() || text.charAt(i) == ' ') {
				lastDelimitedPrefix = i;
			}
			return lastDelimitedPrefix;
		}

		private int lengthOfCommonPrefix(String other) {
			int i = 0;
			for (; i < other.length() && i < prefix.length(); i++) {
				if (other.charAt(i) != prefix.charAt(i)) {
					return i;
				}
			}
			return i;
		}

		@Override
		public String toString() {
			StringBuilder out = new StringBuilder();
			for (RadixTreeNode child : children) {
				out.append('\'').append(child.prefix).append("' = [");
				out.append(child.toString());
				out.append(']');
			}
			return out.toString();
		}
	}

	private static class Match {
		protected RadixTreeNode group;
		protected int prefixLength;

		protected boolean isMatchingCompletePrefix() {
			return group.prefix.length() == prefixLength;
		}
	}

}
