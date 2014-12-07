package org.stt.event.messages;

import org.stt.fun.Achievement;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by dante on 07.12.14.
 */
public class RefreshedAchievements {
    public final Collection<Achievement> reachedAchievements;

    public RefreshedAchievements(Collection<Achievement> reachedAchievements) {

        this.reachedAchievements = Collections.unmodifiableCollection(reachedAchievements);
    }
}
