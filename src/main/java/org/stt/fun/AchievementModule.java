package org.stt.fun;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * Created by dante on 04.12.14.
 */
public class AchievementModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    AchievementService provideAchievements(ResourceBundle resourceBundle, EventBus eventBus) {
        Collection<Achievement> listOfAchievements = new ArrayList<>();
        for (int i : Arrays.asList(11, 31, 101)) {
            listOfAchievements.add(new DaysTrackedAchievement(resourceBundle, i));
        }
        listOfAchievements.add(new LongComments(resourceBundle, 7,
                200));
        listOfAchievements.add(new HoursTrackedAchievement(resourceBundle, 1009));
        listOfAchievements.add(new AmountOfItemsAchievement(resourceBundle, 41));
        return new AchievementService(listOfAchievements, eventBus);
    }
}
