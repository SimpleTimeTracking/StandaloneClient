package org.stt.cli;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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

/**
 * Converts different supported time tracking formats. Currently these are:
 * 
 * - CSV - STT internal format - modified ti format
 */
public class FormatConverter {

	private ItemReader from;
	private ItemWriter to;

	/**
	 * 
	 * @param input
	 *            if null, System.in is assumed
	 * @param output
	 *            if null, System.out is assumed
	 * @param args
	 */
	public FormatConverter(List<String> args) {
		Preconditions.checkNotNull(args);

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
		Reader inputReader = null;
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
			throw new RuntimeException("unknown input format \"" + sourceFormat
					+ "\"");
		}
	}

	public void convert() throws IOException {
		Optional<TimeTrackingItem> current = null;
		while ((current = from.read()).isPresent()) {
			to.write(current.get());
		}

		from.close();
		to.close();
	}
}
