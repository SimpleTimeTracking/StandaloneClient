package org.stt.persistence;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.stt.STTItemWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

/**
 * Created by dante on 23.06.15.
 */
public class DemoDataWriter {
    public static void main(String[] args) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream("testdata.txt"), StandardCharsets.UTF_8)) {
            STTItemWriter itemWriter = new STTItemWriter(writer);
            LocalDateTime time = now().minusHours(10000);
            for (int i = 0; i < 10000; i++) {
                LocalDateTime nextTime = time.plusHours(1);
                TimeTrackingItem item = new TimeTrackingItem("item " + i, time, nextTime);
                itemWriter.write(item);
                time = nextTime;
            }
        }
    }
}
