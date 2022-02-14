package org.maxime.optimistics

import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertTrue

@Suppress("SameParameterValue")
class MergePDFTest {

    @Test
    fun mergePDF() {

        // prepare output stream
        val outputStream = ByteArrayOutputStream()

        // prepare merger
        val merger = PDFMergerUtility()
        merger.destinationStream = outputStream

        // load as bytes
        val pdf1 = getResource("/PDF1.pdf");
        val pdf2 = getResource("/PDF2.pdf");

        // add documents
        merger.addSource(pdf1.inputStream())
        merger.addSource(pdf2.inputStream())

        // merge all
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

        // generate file
        val output = File("target/mergedPDF.pdf")
        output.writeBytes(outputStream.toByteArray())

        assertTrue(output.exists())
    }

    private fun getResource(path: String): ByteArray =
        object {}.javaClass.getResource(path)!!.readBytes()
}