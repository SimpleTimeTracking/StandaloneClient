package org.stt.cli;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.stt.csv.importer.CsvImporter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemWriter;
import org.stt.persistence.stt.STTItemReader;
import org.stt.persistence.stt.STTItemWriter;
import org.stt.ti.importer.TiImporter;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts different supported time tracking formats. Currently these are:
 *
 * - CSV - STT internal asNewItemCommandText - modified ti asNewItemCommandText
 */
public class FormatConverter {

	private ItemReader from;
	private ItemWriter to;

	public FormatConverter(List<String> args) {
        Objects.requireNonNull(args);

		File sourceFile = null;
		String sourceFormat = "stt";
		File targetFile = null;
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

		from = getReaderFrom(sourceFile, sourceFormat);
		to = getWriterFrom(targetFile);
	}

	private ItemWriter getWriterFrom(File output) {
		try {
			if (output == null) {
				return new STTItemWriter(new OutputStreamWriter(System.out,
						"UTF-8"));
			}
			return new STTItemWriter(
					new FileWriterWithEncoding(output, "UTF-8"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private ItemReader getReaderFrom(File input, String sourceFormat) {
        Reader inputReader;
        try {
			inputReader = new InputStreamReader(System.in, "UTF-8");
			if (input != null) {
				inputReader = new InputStreamReader(new FileInputStream(input),
						"UTF-8");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		switch (sourceFormat) {
		case "stt":
			return new STTItemReader(inputReader);
		case "ti":
			return new TiImporter(inputReader);
		case "csv":
			return new CsvImporter(inputReader, 1, 4, 8);
		default:
            throw new RuntimeException("unknown input asNewItemCommandText \"" + sourceFormat
                    + "\"");
        }
	}

    public void convert() {
        Optional<TimeTrackingItem> current;
        while ((current = from.read()).isPresent()) {
            to.write(current.get());
		}

		from.close();
		to.close();
	}
}
