package org.stt.persistence;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.stt.STTItemPersister;
import org.stt.persistence.stt.STTItemWriter;

import java.io.*;

/**
 * Created by dante on 23.06.15.
 */
public class DemoDataWriter {
    public static void main(String[] args) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream("testdata.txt"), "UTF8")) {
            STTItemWriter itemWriter = new STTItemWriter(writer);
            DateTime time = DateTime.now().minusHours(10000);
            for (int i = 0; i < 10000; i++) {
                DateTime nextTime = time.plusHours(1);
                TimeTrackingItem item = new TimeTrackingItem("item " + i, time, nextTime);
                itemWriter.write(item);
                time = nextTime;
            }
        }
    }
}
