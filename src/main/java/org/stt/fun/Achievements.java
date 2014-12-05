package org.stt.fun;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author dante
 */
public class Achievements {

	private Collection<Achievement> achievements = new ArrayList<>();

	public Achievements(Collection<Achievement> achievements) {
		Preconditions.checkNotNull(achievements);
		this.achievements.addAll(achievements);
	}

	public void determineAchievementsFrom(ItemReader reader) {
		for (Achievement achievement : achievements) {
			achievement.start();
		}

		Optional<TimeTrackingItem> read;
		while ((read = reader.read()).isPresent()) {
			for (Achievement achievement : achievements) {
				achievement.process(read.get());
			}
		}

		for (Achievement achievement : achievements) {
			achievement.done();
		}
	}

	public Collection<Achievement> getReachedAchievements() {
		Collection<Achievement> result = new ArrayList<>();
		for (Achievement achievement : achievements) {
			if (achievement.isAchieved()) {
				result.add(achievement);
			}
		}
		return result;
	}
}
