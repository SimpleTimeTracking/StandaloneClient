package org.stt.persistence.stt;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemWriter;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.*;
import java.util.*;

/**
 * Writes {@link TimeTrackingItem}s to a new line. Multiline comments get joined
 * into one line: line endings \r and \n get replaced by the string \r and \n
 * respectively.
 */
@Singleton
public class STTItemPersister implements ItemPersister {

    private final STTItemConverter converter = new STTItemConverter();
    private Provider<Reader> readerProvider;
    private Provider<Writer> writerProvider;

    @Inject
    public STTItemPersister(@STTFile Provider<Reader> readerProvider,
                            @STTFile Provider<Writer> writerProvider) {
        this.readerProvider = Objects.requireNonNull(readerProvider);
        this.writerProvider = Objects.requireNonNull(writerProvider);
    }

    @Override
    public void persist(TimeTrackingItem itemToInsert) {
        Objects.requireNonNull(itemToInsert);
        StringWriter stringWriter = new StringWriter();
        Reader providedReader;
        providedReader = readerProvider.get();
        try (STTItemReader in = new STTItemReader(providedReader);
             STTItemWriter out = new STTItemWriter(stringWriter)) {
            new InsertHelper(in, out, itemToInsert).performInsert();
            rewriteFileWith(stringWriter.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void replace(TimeTrackingItem item, TimeTrackingItem with) {
        delete(item);
        persist(with);
    }

    @Override
    public void delete(TimeTrackingItem item) {
        try (BufferedReader reader = new BufferedReader(readerProvider.get())) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            String lineOfItemToDelete = converter.timeTrackingItemToLine(item);
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                // only persist lines which should not be deleted
                if (!currentLine.equals(lineOfItemToDelete)) {
                    printWriter.println(currentLine);
                }
            }

            printWriter.flush();
            rewriteFileWith(stringWriter.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Collection<UpdatedItem> updateActivitities(Collection<TimeTrackingItem> itemsToUpdate, String newActivity) {
        try (ItemReader in = new STTItemReader(readerProvider.get());
             StringWriter sw = new StringWriter();
             ItemWriter out = new STTItemWriter(sw)) {
            Set<TimeTrackingItem> matchingItems = new HashSet<>(itemsToUpdate);
            List<UpdatedItem> updatedItems = new ArrayList<>();
            Optional<TimeTrackingItem> oItem;
            while ((oItem = in.read()).isPresent()) {
                TimeTrackingItem item = oItem.get();
                if (matchingItems.contains(item)) {
                    TimeTrackingItem updateItem = item.withActivity(newActivity);
                    updatedItems.add(new UpdatedItem(item, updateItem));
                    item = updateItem;
                }
                out.write(item);
            }
            rewriteFileWith(sw.toString());
            return updatedItems;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void rewriteFileWith(String content) throws IOException {
        Writer truncatingWriter = writerProvider.get();
        truncatingWriter.write(content);
        truncatingWriter.close();
    }
}
