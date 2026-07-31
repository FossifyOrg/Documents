package org.fossify.documents.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvDocumentParserTest {
    @Test
    fun `parses quoted commas escaped quotes and embedded newlines`() {
        val table = CsvDocumentParser.parse(
            source = "Name,Notes\nAda,\"Compiler, pioneer\"\nGrace,\"Said \"\"hello\"\"\nfrom COBOL\"",
            fileName = "people.csv",
        )

        assertEquals(2, table.columnCount)
        assertEquals(listOf("Name", "Notes"), table.rows[0])
        assertEquals(listOf("Ada", "Compiler, pioneer"), table.rows[1])
        assertEquals(listOf("Grace", "Said \"hello\"\nfrom COBOL"), table.rows[2])
    }

    @Test
    fun `detects semicolon separated files`() {
        val table = CsvDocumentParser.parse("Name;City\nAda;London\nGrace;New York")

        assertEquals(2, table.columnCount)
        assertEquals(listOf("Grace", "New York"), table.rows.last())
    }

    @Test
    fun `uses tabs for tsv files`() {
        val table = CsvDocumentParser.parse("Name\tScore\nAda\t10", "scores.tsv")

        assertEquals(2, table.columnCount)
        assertEquals(listOf("Ada", "10"), table.rows.last())
    }

    @Test
    fun `keeps uneven rows and strips utf8 bom`() {
        val table = CsvDocumentParser.parse("\uFEFFOne,Two,Three\n1,2\n3,4,5")

        assertEquals(3, table.columnCount)
        assertEquals(listOf("One", "Two", "Three"), table.rows.first())
        assertEquals(listOf("1", "2"), table.rows[1])
    }
}
