package org.fossify.documents.data

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

internal object CsvDocumentParser {
    private val delimiters = listOf(',', ';', '\t', '|')

    fun parse(source: String, fileName: String = ""): StructuredDocumentContent.Table {
        val text = source.removePrefix("\uFEFF")
        val preferredDelimiter = if (fileName.endsWith(".tsv", ignoreCase = true)) '\t' else null
        val delimiter = preferredDelimiter ?: delimiters.maxByOrNull { candidate ->
            runCatching {
                delimiterScore(parseSampleRows(text, candidate))
            }.getOrDefault(Int.MIN_VALUE)
        } ?: ','
        val rows = parseRows(text, delimiter)

        return StructuredDocumentContent.Table(
            rows = rows,
            columnCount = rows.maxOfOrNull { it.size } ?: 0,
        )
    }

    private fun parseRows(source: String, delimiter: Char): List<List<String>> {
        return CSVParser.parse(source, csvFormat(delimiter)).use { parser ->
            parser.map { record -> record.toList() }
        }
    }

    private fun parseSampleRows(source: String, delimiter: Char): List<List<String>> {
        return CSVParser.parse(source, csvFormat(delimiter)).use { parser ->
            parser.asSequence()
                .take(SAMPLE_ROW_COUNT)
                .map { record -> record.toList() }
                .toList()
        }
    }

    private fun csvFormat(delimiter: Char): CSVFormat {
        return CSVFormat.DEFAULT.builder()
            .setDelimiter(delimiter)
            .setIgnoreEmptyLines(false)
            .get()
    }

    private fun delimiterScore(rows: List<List<String>>): Int {
        if (rows.isEmpty()) {
            return 0
        }

        val widths = rows.map { it.size }
        val mostCommonWidth = widths.groupingBy { it }.eachCount().maxByOrNull { it.value }
            ?: return 0
        if (mostCommonWidth.key <= 1) {
            return 0
        }

        return mostCommonWidth.key * COLUMN_SCORE_WEIGHT +
                mostCommonWidth.value * CONSISTENCY_SCORE_WEIGHT - widths.distinct().size
    }

    private const val SAMPLE_ROW_COUNT = 24
    private const val COLUMN_SCORE_WEIGHT = 100
    private const val CONSISTENCY_SCORE_WEIGHT = 10
}
