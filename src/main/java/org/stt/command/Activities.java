package org.stt.command;

import net.engio.mbassy.bus.MBassador;
import org.stt.model.ItemDeleted;
import org.stt.model.ItemInserted;
import org.stt.model.ItemReplaced;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.query.TimeTrackingItemQueries;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Command "side" of tracking activities
 */
@Singleton
public class Activities implements CommandHandler {
    private ItemPersister persister;
    private final TimeTrackingItemQueries queries;
    private Optional<MBassador<Object>> eventBus;

    @Inject
    public Activities(ItemPersister persister,
                      TimeTrackingItemQueries queries,
                      Optional<MBassador<Object>> eventBus) {
        this.eventBus = requireNonNull(eventBus);
        this.persister = requireNonNull(persister);
        this.queries = requireNonNull(queries);
    }

    @Override
    public void addNewActivity(NewActivity command) {
        requireNonNull(command);
        persister.persist(command.newItem);
        eventBus.ifPresent(eb -> eb.publish(new ItemInserted(command.newItem)));
    }

    @Override
    public void endCurrentActivity(EndCurrentItem command) {
        requireNonNull(command);
        queries.getOngoingItem()
                .ifPresent(item -> {
                    TimeTrackingItem derivedItem = item.withEnd(command.endAt);
                    persister.replace(item, derivedItem);
                    eventBus.ifPresent(eb -> eb.publish(new ItemReplaced(item, derivedItem)));
                });
    }

    @Override
    public void removeActivity(RemoveActivity command) {
        requireNonNull(command);
        persister.delete(command.itemToDelete);
        eventBus.ifPresent(eb -> eb.publish(new ItemDeleted(command.itemToDelete)));
    }

    @Override
    public void removeActivityAndFillGap(RemoveActivity command) {
        requireNonNull(command);
        queries.getAdjacentItems(command.itemToDelete);
    }

    @Override
    public void resumeActivity(ResumeActivity command) {
        requireNonNull(command);
        TimeTrackingItem resumedItem = command.itemToResume
                .withPendingEnd()
                .withStart(command.beginningWith);
        persister.persist(resumedItem);
        eventBus.ifPresent(eb -> eb.publish(new ItemInserted(resumedItem)));
    }

    @Override
    public void resumeLastActivity(ResumeLastActivity command) {
        requireNonNull(command);

        Optional<TimeTrackingItem> lastTimeTrackingItem = queries.getLastItem();
        if (lastTimeTrackingItem.isPresent() && lastTimeTrackingItem.get().getEnd().isPresent()) {
            lastTimeTrackingItem.ifPresent(timeTrackingItem -> {
                TimeTrackingItem resumedItem = timeTrackingItem.withPendingEnd().withStart(command.resumeAt);
                persister.persist(resumedItem);
                eventBus.ifPresent(eb -> eb.publish(new ItemInserted(resumedItem)));
            });
        }
    }
}