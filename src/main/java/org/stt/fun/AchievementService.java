package org.stt.fun;

import com.google.common.eventbus.EventBus;
import org.stt.Service;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

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
    private TimeTrackingItemQueries searcher;

    public AchievementService(Collection<Achievement> achievements, EventBus eventBus, TimeTrackingItemQueries searcher) {
        this.searcher = checkNotNull(searcher);
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

	private void dispatchSuccessfulAchievements() {
		eventBus.post(new AchievementsUpdated());
	}

    public Collection<Achievement> getReachedAchievements() {
        ArrayList<Achievement> result = new ArrayList<>();
        for (Achievement achievement: achievements) {
            if (achievement.isAchieved()) {
                result.add(achievement);
            }
        }
        return result;
    }

	@Override
	public void start() throws Exception {
		eventBus.register(this);
        calculateAchievements();
	}

    private void calculateAchievements() {
        resetAchievements();
        for (TimeTrackingItem item: searcher.queryAllItems()) {
            for (Achievement achievement: achievements) {
                achievement.process(item);
            }
        }

        finishAchievements();
        dispatchSuccessfulAchievements();
    }

    @Override
	public void stop() {
		eventBus.unregister(this);
	}
}
