package org.stt.persistence;

import java.io.InputStream;
import java.util.Collection;

import org.stt.model.TimeTrackingItem;

/**
 */
public interface ItemReader
{
   /**
    * Reads all items from the given InputStream.
    * 
    * @return 
    */
   public Collection<TimeTrackingItem> readItemsFrom(InputStream in);
}
