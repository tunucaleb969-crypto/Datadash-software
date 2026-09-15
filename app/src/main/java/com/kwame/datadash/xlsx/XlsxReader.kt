package com.kwame.datadash.xlsx

import android.content.Context
import android.net.Uri
import com.kwame.datadash.data.Entry
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * Minimal, dependency-free XLSX reader. Uses only Android's built-in
 * XmlPullParser and java.util.zip, so it can read both files this app
 * exports (inline strings) and typical Excel-exported files (shared
 * strings). Assumes a single worksheet, which covers ordinary
 * small-business spreadsheets.
 */
object XlsxReader {

    fun read(context: Context, uri: Uri): List<Entry> {
        val result = mutableListOf<Entry>()
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return result
            var sharedStrings: List<String> = emptyList()
            var sheetXml: String? = null

            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                val sharedStringsBytes = ByteArrayOutputStream()
                val sheetBytes = ByteArrayOutputStream()
                while (entry != null) {
                    when {
                        entry.name == "xl/sharedStrings.xml" -> zip.copyTo(sharedStringsBytes)
                        entry.name.startsWith("xl/worksheets/sheet") -> zip.copyTo(sheetBytes)
                    }
                    entry = zip.nextEntry
                }
                if (sharedStringsBytes.size() > 0) {
                    sharedStrings = parseSharedStrings(sharedStringsBytes.toString("UTF-8"))
                }
                if (sheetBytes.size() > 0) {
                    sheetXml = sheetBytes.toString("UTF-8")
                }
            }

            sheetXml?.let { xml ->
                val rows = parseSheetRows(xml, sharedStrings)
                if (rows.isNotEmpty()) {
                    val dataRows = rows.drop(1)
                    dataRows.forEach { cells ->
                        if (cells.isNotEmpty() && cells[0].isNotBlank()) {
                            result.add(
                                Entry(
                                    name = cells.getOrElse(0) { "" },
                                    phone = cells.getOrElse(1) { "" },
                                    category = cells.getOrElse(2) { "" },
                                    amount = cells.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0,
                                    date = cells.getOrElse(4) { "" },
                                    notes = cells.getOrElse(5) { "" }
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Return whatever was successfully parsed before the failure
        }
        return result
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val list = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())
        var eventType = parser.eventType
        val sb = StringBuilder()
        var insideSi = false
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        insideSi = true
                        sb.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideSi) sb.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        list.add(sb.toString())
                        insideSi = false
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun parseSheetRows(xml: String, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())
        var eventType = parser.eventType

        var currentRow: MutableList<String>? = null
        var currentCellType: String? = null
        var lastColIndex = -1
        var sb = StringBuilder()
        var insideValueOrText = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                            lastColIndex = -1
                        }
                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t")
                            val ref = parser.getAttributeValue(null, "r")
                            val currentColIndex = ref?.let { columnIndexFromRef(it) } ?: (lastColIndex + 1)
                            while (lastColIndex + 1 < currentColIndex) {
                                currentRow?.add("")
                                lastColIndex++
                            }
                            lastColIndex = currentColIndex
                        }
                        "v", "t" -> {
                            insideValueOrText = true
                            sb = StringBuilder()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideValueOrText) sb.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v", "t" -> {
                            insideValueOrText = false
                        }
                        "c" -> {
                            val raw = sb.toString()
                            val value = if (currentCellType == "s") {
                                raw.toIntOrNull()?.let { sharedStrings.getOrElse(it) { "" } } ?: ""
                            } else raw
                            currentRow?.add(value)
                            sb = StringBuilder()
                        }
                        "row" -> {
                            currentRow?.let { rows.add(it) }
                            currentRow = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun columnIndexFromRef(ref: String): Int {
        var index = 0
        for (ch in ref) {
            if (ch.isLetter()) {
                index = index * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else break
        }
        return index - 1
    }
}
