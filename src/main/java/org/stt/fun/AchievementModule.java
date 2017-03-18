package org.stt.fun;

import dagger.Module;
import dagger.Provides;
import net.engio.mbassy.bus.MBassador;
import org.stt.query.TimeTrackingItemQueries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

@Module
public class AchievementModule {
    private AchievementModule() {
    }

    @Provides
    static AchievementService provideAchievements(ResourceBundle resourceBundle, MBassador<Object> eventBus, TimeTrackingItemQueries timeTrackingItemQueries) {
        Collection<Achievement> listOfAchievements = new ArrayList<>();
        for (int i : Arrays.asList(11, 31, 101)) {
            listOfAchievements.add(new DaysTrackedAchievement(resourceBundle, i));
        }
        listOfAchievements.add(new LongComments(resourceBundle, 7,
                200));
        listOfAchievements.add(new HoursTrackedAchievement(resourceBundle, 1009));
        listOfAchievements.add(new AmountOfItemsAchievement(resourceBundle, 41));
        return new AchievementService(listOfAchievements, eventBus, timeTrackingItemQueries);
    }
}
