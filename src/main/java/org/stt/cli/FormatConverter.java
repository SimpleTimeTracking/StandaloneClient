package org.stt.cli;

import org.stt.csv.importer.CsvImporter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemWriter;
import org.stt.persistence.stt.STTItemReader;
import org.stt.persistence.stt.STTItemWriter;
import org.stt.ti.importer.TiImporter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts different supported time tracking formats. Currently these are:
 * <p>
 * - CSV - STT internal asNewItemCommandText - modified ti asNewItemCommandText
 */
class FormatConverter {

    private File targetFile;
    private File sourceFile;
    private String sourceFormat;

    FormatConverter(List<String> args) {
        Objects.requireNonNull(args);

        sourceFile = null;
        sourceFormat = "stt";
        targetFile = null;
        int sourceFormatIndex = args.indexOf("--sourceFormat");
        if (sourceFormatIndex != -1) {
            args.remove(sourceFormatIndex);
            sourceFormat = args.get(sourceFormatIndex);
            args.remove(sourceFormatIndex);
        }
        int sourceIndex = args.indexOf("--source");
        if (sourceIndex != -1) {
            args.remove(sourceIndex);
            sourceFile = new File(args.get(sourceIndex));
            args.remove(sourceIndex);
        }
        int targetIndex = args.indexOf("--target");
        if (targetIndex != -1) {
            args.remove(targetIndex);
            targetFile = new File(args.get(targetIndex));
            args.remove(targetIndex);
        }
    }

    private ItemWriter getWriterFrom(File output) throws FileNotFoundException {
        if (output == null) {
            return new STTItemWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)); // NOSONAR not logging
        }
        return new STTItemWriter(
                new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8));
    }

    private ItemReader getReaderFrom(File input, String sourceFormat) throws IOException {
        Reader inputReader;
        if (input == null) {
            inputReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        } else {
            inputReader = new InputStreamReader(new FileInputStream(input), StandardCharsets.UTF_8);
        }

        switch (sourceFormat) {
            case "stt":
                return new STTItemReader(inputReader);
            case "ti":
                return new TiImporter(inputReader);
            case "csv":
                return new CsvImporter(inputReader, 1, 4, 8);
            default:
                inputReader.close();
                throw new InvalidSourceFormatException("unknown input asNewItemCommandText \"" + sourceFormat
                        + "\"");
        }
    }

    void convert() {
        try (ItemReader from = getReaderFrom(sourceFile, sourceFormat);
             ItemWriter to = getWriterFrom(targetFile)) {

            Optional<TimeTrackingItem> current;
            while ((current = from.read()).isPresent()) {
                to.write(current.get());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private class InvalidSourceFormatException extends RuntimeException {
        InvalidSourceFormatException(String message) {
            super(message);
        }
    }
}
