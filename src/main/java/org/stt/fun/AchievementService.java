package org.stt.fun;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.stt.Service;
import org.stt.event.messages.ReadItemsResult;
import org.stt.event.messages.RefreshedAchievements;
import org.stt.model.TimeTrackingItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * @author dante
 */
public class AchievementService implements Service {
	private static final Logger LOG = Logger.getLogger(AchievementService.class.getName());

	private Collection<Achievement> achievements = new ArrayList<>();
	private EventBus eventBus;

	public AchievementService(Collection<Achievement> achievements, EventBus eventBus) {
		this.eventBus = checkNotNull(eventBus);
		this.achievements.addAll(checkNotNull(achievements));
	}

	private void finishAchievements() {
		for (Achievement achievement : achievements) {
			achievement.done();
		}
	}

	private void resetAchievements() {
		for (Achievement achievement : achievements) {
			achievement.start();
		}
	}

	private Collection<Achievement> getReachedAchievements() {
		Collection<Achievement> result = new ArrayList<>();
		for (Achievement achievement : achievements) {
			if (achievement.isAchieved()) {
				result.add(achievement);
			}
		}
		return result;
	}

	@Subscribe
	public synchronized void onItemsRead(ReadItemsResult event) {
        resetAchievements();

		for (TimeTrackingItem item: event.timeTrackingItems) {
			for (Achievement achievement: achievements) {
				achievement.process(item);
			}
		}

        finishAchievements();
        dispatchSuccessfulAchievements();
	}

	private void dispatchSuccessfulAchievements() {
		eventBus.post(new RefreshedAchievements(getReachedAchievements()));
	}


	@Override
	public void start() throws Exception {
		eventBus.register(this);
	}

	@Override
	public void stop() {
		eventBus.unregister(this);
	}
}
