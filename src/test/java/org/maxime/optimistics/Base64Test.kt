package org.maxime.optimistics

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

@Suppress("SameParameterValue")
class Base64Test {

    @Test
    fun sizeOfPdf() {

        // Load as bytes
        val bytes: ByteArray = getResource("/PdfOf1Mb.pdf");
        val bytesSize = 1042925;
        Assertions.assertEquals(bytesSize, bytes.size)

        // Convert to base64
        val base64: String = Base64.getEncoder().encodeToString(bytes)
        val base64Size = 1390568;
        Assertions.assertEquals(base64Size, base64.toByteArray().size);

        // Revert to bytes
        val bytes2: ByteArray = Base64.getDecoder().decode(base64)
        Assertions.assertEquals(bytesSize, bytes2.size);

        // Chek ratio
        val increasedSize = (base64Size - bytesSize) * 100 / bytesSize
        Assertions.assertEquals(33, increasedSize)
    }

    private fun getResource(path: String): ByteArray =
        object {}.javaClass.getResource(path)!!.readBytes()
}