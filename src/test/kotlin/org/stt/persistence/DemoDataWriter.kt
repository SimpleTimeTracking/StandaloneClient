package org.stt.persistence

import org.stt.model.TimeTrackingItem
import org.stt.persistence.stt.STTItemWriter
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime.now

/**
 * Created by dante on 23.06.15.
 */
object DemoDataWriter {
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        OutputStreamWriter(FileOutputStream("testdata.txt"), StandardCharsets.UTF_8).use { writer ->
            val itemWriter = STTItemWriter(writer)
            var time = now().minusHours(10000)
            for (i in 0..9999) {
                val nextTime = time.plusHours(1)
                val item = TimeTrackingItem("item $i", time, nextTime)
                itemWriter.write(item)
                time = nextTime
            }
        }
    }
}
