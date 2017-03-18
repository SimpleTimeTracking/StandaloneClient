package org.stt.fun;

import net.engio.mbassy.bus.MBassador;
import org.stt.Service;
import org.stt.query.TimeTrackingItemQueries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 *
 * @author dante
 */
public class AchievementService implements Service {
	private Collection<Achievement> achievements = new ArrayList<>();
    private MBassador<Object> eventBus;
    private TimeTrackingItemQueries searcher;

    public AchievementService(Collection<Achievement> achievements, MBassador eventBus, TimeTrackingItemQueries searcher) {
        this.searcher = Objects.requireNonNull(searcher);
        this.eventBus = Objects.requireNonNull(eventBus);
        this.achievements.addAll(Objects.requireNonNull(achievements));
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
        eventBus.publish(new AchievementsUpdated());
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
        eventBus.subscribe(this);
        calculateAchievements();
	}

    private void calculateAchievements() {
        resetAchievements();
        searcher.queryAllItems()
                .forEach(item ->
                        achievements.forEach(achievement -> achievement.process(item)));

        finishAchievements();
        dispatchSuccessfulAchievements();
    }

    @Override
	public void stop() {
        eventBus.unsubscribe(this);
    }
}
