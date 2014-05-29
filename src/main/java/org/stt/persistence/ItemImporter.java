package org.stt.persistence;

import java.util.Collection;

import org.stt.model.TimeTrackingItem;

/**
 * All classes implementing import functionality from other systems should import this interface.
 */
public interface ItemImporter
{
   Collection<TimeTrackingItem> read();
}
