package com.kwame.datadash.xlsx

import android.content.Context
import android.net.Uri
import com.kwame.datadash.data.Entry
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal, dependency-free XLSX writer. Writes a single-sheet workbook
 * using inline strings (no sharedStrings.xml needed) so the whole file
 * format is under our control — no heavy third-party Excel library
 * required on Android.
 */
object XlsxWriter {

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private const val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Entries" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private const val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

    private val HEADERS = listOf("Name", "Phone", "Category", "Amount", "Date", "Notes")

    fun write(context: Context, uri: Uri, entries: List<Entry>): Boolean {
        return try {
            val out: OutputStream = context.contentResolver.openOutputStream(uri) ?: return false
            ZipOutputStream(out).use { zip ->
                writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES)
                writeZipEntry(zip, "_rels/.rels", RELS)
                writeZipEntry(zip, "xl/workbook.xml", WORKBOOK)
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
                writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildSheetXml(entries))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildSheetXml(entries: List<Entry>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")

        sb.append(rowXml(1, HEADERS))
        entries.forEachIndexed { index, entry ->
            val values = listOf(
                entry.name, entry.phone, entry.category,
                entry.amount.toString(), entry.date, entry.notes
            )
            sb.append(rowXml(index + 2, values))
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun rowXml(rowIndex: Int, values: List<String>): String {
        val sb = StringBuilder("<row r=\"$rowIndex\">")
        values.forEachIndexed { colIndex, value ->
            val cellRef = columnLetter(colIndex) + rowIndex.toString()
            sb.append(
                """<c r="$cellRef" t="inlineStr"><is><t xml:space="preserve">${'$'}{escapeXml(value)}</t></is></c>"""
            )
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun columnLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
        } while (i >= 0)
        return sb.toString()
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
