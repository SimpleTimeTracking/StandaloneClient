package org.stt.persistence;

import java.io.IOException;
import java.util.Collection;

import org.stt.model.TimeTrackingItem;

/**
 * All classes implementing import functionality from other systems should import this interface.
 */
public interface ItemImporter
{
   /**
    * @return all items in the order of the source, e.g. 
    * @throws IOException if something goes wrong while reading 
    */
   Collection<TimeTrackingItem> read() throws IOException;
}
