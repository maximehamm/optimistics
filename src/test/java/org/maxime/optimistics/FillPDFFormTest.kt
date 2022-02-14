package org.maxime.optimistics

import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.test.assertTrue


@Suppress("SameParameterValue")
class FillPDFFormTest {

    /**
     * PDFBox documentation here :
     * https://pdfbox.apache.org/1.8/cookbook/fill-form-field.html
     */
    @Test
    fun sizeOfPdf() {

        // load as bytes
        val pdfAsBytes: ByteArray = getResource("/pdf-form-template.pdf");
        val pdfDocument: PDDocument = PDDocument.load(pdfAsBytes);

        // get the document catalog
        val docCatalog = pdfDocument.documentCatalog
        val form = docCatalog.acroForm

        // Dump to System.out
        form.fields.forEach { field ->
            println(field.fullyQualifiedName + ": " + field.fieldType)
        }

        // list fields
        assertEquals(18, form.fields.size)
        assertTrue(form.getField("City") != null)
        assertTrue(form.getField("PostalCode") != null)
        assertTrue(form.getField("Address") != null)
        assertTrue(form.getField("PhoneNumber") != null)

        // Update field document
        form.getField("LastName").setValue("WANNEROY")
        form.getField("FirstName").setValue("Raphaël")
        form.getField("City").setValue("Saint-Denis")
        form.getField("PostalCode").setValue("93200")
        form.getField("Address").setValue("32 rue de l'Esperluète")
        form.getField("PhoneNumber").setValue("06 06 06 06 06")

        // Update check box
        form.getField("CurrentlyWorking").setValue("Yes")

        // Save document
        val file = File("pdf-form-wanneroy.pdf").apply { createNewFile() }
        pdfDocument.save(file);
        pdfDocument.close();
    }

    private fun getResource(path: String): ByteArray =
        object {}.javaClass.getResource(path)!!.readBytes()
}