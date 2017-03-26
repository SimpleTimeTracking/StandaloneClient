package org.stt.csv.importer;

import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;

/**
 * Created by tw on 25.03.17.
 */
public class CsvImporterTest {

    CsvImporter sut;

    @Test
    public void canReadLine() {
        Reader inputReader = new StringReader("abc;\"02.01.2017\";\"Mo\";;\"01:10\";\"some stuff\";\"other stuff\";\"stuff\";\"work item text\";");
        sut = new CsvImporter(inputReader, 1, 4, 8);
        sut.read();
    }
}
