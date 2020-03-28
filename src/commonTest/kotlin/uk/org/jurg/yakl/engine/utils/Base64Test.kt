/*
 * Copyright 2020 Chris Clifton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
