package org.stt.importer;

import java.util.Collection;

import org.stt.model.TimeTrackingItem;

/**
 * All classes implementing export functionality to other systems should import this interface.
 */
public interface ItemExporter
{
   /**
    * Write all elements of the given Collection.
    */
   void write(Collection<TimeTrackingItem> toWrite);
}
