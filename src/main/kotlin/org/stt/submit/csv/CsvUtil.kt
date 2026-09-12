package org.stt.submit.csv

/**
 * Utilities for hand-building CSV output (no external CSV library, see design decision 4 in
 * the csv-submit-connectors change).
 */
object CsvUtil {

    private val specialChars = charArrayOf(',', '"', '\n', '\r')

    /**
     * Escapes a single CSV cell value. Values containing commas, quotes, or line breaks are
     * wrapped in double quotes; embedded quotes are doubled (`"` becomes `""`).
     */
    fun escapeCell(value: String): String {
        val needsQuoting = value.indexOfAny(specialChars) >= 0
        if (!needsQuoting) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
