package uk.org.jurg.yakl.engine.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Base64Test {

    val a = 'a'.toByte()

    @Test
    fun `base64 encode with padding`() {
        val bytes = byteArrayOf(a)
        assertEquals(bytes.base64EncodeToString(), "YQ==")
    }

    @Test
    fun `base64 encode with no padding`() {
        val bytes = byteArrayOf(a, a, a)
        assertEquals(bytes.base64EncodeToString(), "YWFh")
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `base64 encode start of protected area`() {
        val ba = "\u0096".encodeToByteArray()
        assertEquals(ba.base64EncodeToString(), "wpY=")
    }

    @Test
    fun `base64 decode with padding`() {
        assertTrue("YQ==".base64Decode().contentEquals(byteArrayOf(a)))
    }

    @Test
    fun `base64 decode with no padding`() {
        assertTrue("YWFh".base64Decode().contentEquals(byteArrayOf(a, a, a)))
    }
}
