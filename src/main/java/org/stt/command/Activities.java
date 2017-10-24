package org.stt.command;

import net.engio.mbassy.bus.BusRuntime;
import net.engio.mbassy.bus.IMessagePublication;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.common.PubSubSupport;
import org.stt.model.ItemDeleted;
import org.stt.model.ItemInserted;
import org.stt.model.ItemReplaced;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.query.Criteria;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Command "side" of tracking activities
 */
@Singleton
public class Activities implements CommandHandler {
    private ItemPersister persister;
    private final TimeTrackingItemQueries queries;
    private final PubSubSupport<Object> publisher;

    @Inject
    public Activities(ItemPersister persister,
                      TimeTrackingItemQueries queries,
                      Optional<MBassador<Object>> publisher) {
        this.publisher = requireNonNull(publisher).map(PubSubSupport.class::cast)
                .orElseGet(DoNotPublish::new);
        this.persister = requireNonNull(persister);
        this.queries = requireNonNull(queries);
    }

    @Override
    public void addNewActivity(NewActivity command) {
        requireNonNull(command);
        TimeTrackingItem newItem = command.newItem;
        Optional<TimeTrackingItem> potentialItemToReplace = ongoingItemThatWouldEnd(newItem);
        if (!potentialItemToReplace.isPresent()) {
            potentialItemToReplace = itemWithEditedActivity(newItem);
        }

        if (potentialItemToReplace.isPresent()) {
            TimeTrackingItem itemToReplace = potentialItemToReplace.get();
            persister.replace(itemToReplace, command.newItem);
            publisher.publish(new ItemReplaced(itemToReplace, command.newItem));
        } else {
            persister.persist(command.newItem);
            publisher.publish(new ItemInserted(command.newItem));
        }
    }

    private Optional<TimeTrackingItem> itemWithEditedActivity(TimeTrackingItem newItem) {
        Criteria criteria = new Criteria()
                .withStartsAt(newItem.getStart());
        newItem.getEnd().ifPresent(criteria::withEndsAt);
        return queries.queryItems(criteria).findAny();
    }

    private Optional<TimeTrackingItem> ongoingItemThatWouldEnd(TimeTrackingItem newItem) {
        return queries.getLastItem()
                .filter(timeTrackingItem ->
                        !timeTrackingItem.getEnd().isPresent()
                                && newItem.getStart().equals(timeTrackingItem.getStart()));
    }

    @Override
    public void endCurrentActivity(EndCurrentItem command) {
        requireNonNull(command);
        queries.getOngoingItem()
                .ifPresent(item -> {
                    TimeTrackingItem derivedItem = item.withEnd(command.endAt);
                    persister.replace(item, derivedItem);
                    publisher.publish(new ItemReplaced(item, derivedItem));
                });
    }

    @Override
    public void removeActivity(RemoveActivity command) {
        requireNonNull(command);
        persister.delete(command.itemToDelete);
        publisher.publish(new ItemDeleted(command.itemToDelete));
    }

    @Override
    public void removeActivityAndCloseGap(RemoveActivity command) {
        requireNonNull(command);
        TimeTrackingItemQueries.AdjacentItems adjacentItems = queries.getAdjacentItems(command.itemToDelete);
        Optional<TimeTrackingItem> previous = adjacentItems.previousItem();
        Optional<TimeTrackingItem> next = adjacentItems.nextItem();

        if (previous.isPresent()) {
            TimeTrackingItem previousItem = previous.get();
            if (next.isPresent() && previousAndNextActivitiesMatch(previousItem, next.get())) {
                TimeTrackingItem replaceAllWith = next.get().getEnd()
                        .map(previousItem::withEnd).orElse(previousItem.withPendingEnd());
                persister.persist(replaceAllWith);
                publisher.publish(new ItemInserted(replaceAllWith));
                return;
            }
            if (DateTimes.isOnSameDay(previousItem.getStart(), command.itemToDelete.getStart())
                    && !command.itemToDelete.getEnd().isPresent()) {
                TimeTrackingItem replaceAllWith = previousItem.withPendingEnd();
                persister.persist(replaceAllWith);
                publisher.publish(new ItemInserted(replaceAllWith));
                return;
            }
        }
        removeActivity(command);
    }

    private boolean previousAndNextActivitiesMatch(TimeTrackingItem previousItem, TimeTrackingItem nextItem) {
        return previousItem.getActivity().equals(nextItem.getActivity());
    }

    @Override
    public void resumeActivity(ResumeActivity command) {
        requireNonNull(command);
        TimeTrackingItem resumedItem = command.itemToResume
                .withPendingEnd()
                .withStart(command.beginningWith);
        persister.persist(resumedItem);
        publisher.publish(new ItemInserted(resumedItem));
    }

    @Override
    public void resumeLastActivity(ResumeLastActivity command) {
        requireNonNull(command);

        Optional<TimeTrackingItem> lastTimeTrackingItem = queries.getLastItem();
        if (lastTimeTrackingItem.isPresent() && lastTimeTrackingItem.get().getEnd().isPresent()) {
            lastTimeTrackingItem.ifPresent(timeTrackingItem -> {
                TimeTrackingItem resumedItem = timeTrackingItem.withPendingEnd().withStart(command.resumeAt);
                persister.persist(resumedItem);
                publisher.publish(new ItemInserted(resumedItem));
            });
        }
    }

    @Override
    public void bulkChangeActivity(Collection<TimeTrackingItem> itemsToChange, String activity) {
        requireNonNull(itemsToChange);
        requireNonNull(activity);
        Collection<ItemPersister.UpdatedItem> updatedItems = persister.updateActivitities(itemsToChange, activity);
        updatedItems.stream()
                .map(updatedItem -> new ItemReplaced(updatedItem.original, updatedItem.updated))
                .forEach(publisher::publish);
    }

    private static class DoNotPublish implements PubSubSupport<Object> {
        @Override
        public void subscribe(Object listener) {
            throw new IllegalStateException();
        }

        @Override
        public boolean unsubscribe(Object listener) {
            throw new IllegalStateException();
        }

        @Override
        public IMessagePublication publish(Object message) {
            return null;
        }

        @Override
        public BusRuntime getRuntime() {
            throw new IllegalStateException();
        }
    }
}