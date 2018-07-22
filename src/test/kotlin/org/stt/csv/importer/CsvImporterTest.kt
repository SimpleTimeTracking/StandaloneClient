package org.stt.csv.importer

import org.junit.Test
import java.io.StringReader

/**
 * Created by tw on 25.03.17.
 */
class CsvImporterTest {

    internal lateinit var sut: CsvImporter

    @Test
    fun canReadLine() {
        val inputReader = StringReader("abc;\"02.01.2017\";\"Mo\";;\"01:10\";\"some stuff\";\"other stuff\";\"stuff\";\"work item text\";")
        sut = CsvImporter(inputReader, 1, 4, 8)
        sut.read()
    }
}
