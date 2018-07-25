package org.stt.cli

import org.stt.csv.importer.CsvImporter
import org.stt.persistence.ItemReader
import org.stt.persistence.ItemWriter
import org.stt.persistence.stt.STTItemReader
import org.stt.persistence.stt.STTItemWriter
import org.stt.ti.importer.TiImporter
import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Converts different supported time tracking formats. Currently these are:
 *
 *
 * - CSV - STT internal asNewItemCommandText - modified ti asNewItemCommandText
 */
internal class FormatConverter(args: MutableList<String>) {

    private var targetFile: File? = null
    private var sourceFile: File? = null
    private var sourceFormat: String? = null

    init {
        sourceFile = null
        sourceFormat = "stt"
        targetFile = null
        val sourceFormatIndex = args.indexOf("--sourceFormat")
        if (sourceFormatIndex != -1) {
            args.removeAt(sourceFormatIndex)
            sourceFormat = args[sourceFormatIndex]
            args.removeAt(sourceFormatIndex)
        }
        val sourceIndex = args.indexOf("--source")
        if (sourceIndex != -1) {
            args.removeAt(sourceIndex)
            sourceFile = File(args[sourceIndex])
            args.removeAt(sourceIndex)
        }
        val targetIndex = args.indexOf("--target")
        if (targetIndex != -1) {
            args.removeAt(targetIndex)
            targetFile = File(args[targetIndex])
            args.removeAt(targetIndex)
        }
    }

    private fun getWriterFrom(output: File?): ItemWriter {
        return if (output == null) {
            STTItemWriter(OutputStreamWriter(System.out, StandardCharsets.UTF_8)) // NOSONAR not logging
        } else STTItemWriter(
                OutputStreamWriter(FileOutputStream(output), StandardCharsets.UTF_8))
    }

    private fun getReaderFrom(input: File?, sourceFormat: String?): ItemReader {
        val inputReader: Reader
        if (input == null) {
            inputReader = InputStreamReader(System.`in`, StandardCharsets.UTF_8)
        } else {
            inputReader = InputStreamReader(FileInputStream(input), StandardCharsets.UTF_8)
        }

        when (sourceFormat) {
            "stt" -> return STTItemReader(inputReader)
            "ti" -> return TiImporter(inputReader)
            "csv" -> return CsvImporter(inputReader, 1, 4, 8)
            else -> {
                inputReader.close()
                throw InvalidSourceFormatException("unknown input asNewItemCommandText \"" + sourceFormat
                        + "\"")
            }
        }
    }

    fun convert() {
        try {
            getReaderFrom(sourceFile, sourceFormat).use { from ->
                getWriterFrom(targetFile).use { to ->

                    while (true) {
                        from.read()?.let { to.write(it) } ?: break
                    }
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    private class InvalidSourceFormatException internal constructor(message: String) : RuntimeException(message)
}
