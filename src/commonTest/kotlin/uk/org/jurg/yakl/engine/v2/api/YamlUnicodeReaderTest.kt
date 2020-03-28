/*
 * Copyright (c) 2018, http://www.snakeyaml.org
 * Copyright (c) 2020, Chris Clifton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.jurg.yakl.engine.v2.api

@ExperimentalStdlibApi
internal class YamlUnicodeReaderTest {
// TODO   @Test
//    fun `Detect UTF-8 dy default`() {
//        val input = ByteArrayInput("1".encodeToByteArray())
//        val reader = YamlUnicodeReader(input)
//        reader.init()
//        assertEquals(StandardCharsets.UTF_8, reader.getEncoding(), "no BOM must be detected as UTF-8")
//    }
//
//    @Test
//    fun `Detect UTF-8 - EF BB BF`() {
//        val input = ByteArrayInput(
//            byteArrayOf(
//                0xEF.toByte(),
//                0xBB.toByte(),
//                0xBF.toByte(),
//                49.toByte()
//            )
//        )
//        val reader = YamlUnicodeReader(input)
//        reader.init()
//        assertEquals(StandardCharsets.UTF_8, reader.getEncoding(), "no BOM must be detected as UTF-8")
//        assertEquals('1', reader.read(), "BOM must be skipped, #49 -> 1")
//    }
//
//    @Test
//    fun `Detect 00 00 FE FF, UTF-32, big-endian`() {
//        val input = ByteArrayInput(
//            byteArrayOf(
//                0x00.toByte(), 0x00.toByte(), 0xFE.toByte(), 0xFF.toByte(),
//                0.toByte(), 0.toByte(), 0.toByte(), 49.toByte()
//            )
//        )
//        val reader = YamlUnicodeReader(input)
//        reader.init()
//        assertEquals(Charset.forName("UTF-32BE"), reader.getEncoding())
//        assertEquals('1', reader.read(), "BOM must be skipped, #49 -> 1")
//    }
//
//    @Test
//    fun `Detect FF FE 00 00, UTF-32, little-endian`() {
//        val input = ByteArrayInput(
//            byteArrayOf(
//                0xFF.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x00.toByte(),
//                49.toByte(), 0.toByte(), 0.toByte(), 0.toByte()
//            )
//        )
//        val reader = YamlUnicodeReader(input)
//        reader.init()
//        assertEquals(Charset.forName("UTF-32LE"), reader.getEncoding())
//        assertEquals('1', reader.read(), "BOM must be skipped, #49 -> 1")
//    }
//
//    @Test
//    fun `Detect FE FF, UTF-16, big-endian`() {
//        val input = ByteArrayInput(
//            byteArrayOf(
//                0xFE.toByte(), 0xFF.toByte(),
//                0.toByte(), 49.toByte()
//            )
//        )
//        val reader = YamlUnicodeReader(input)
//        reader.init()
//        assertEquals(StandardCharsets.UTF_16BE, reader.getEncoding())
//        assertEquals('1', reader.read(), "BOM must be skipped, #49 -> 1")
//    }
//
//    @Test
//    fun `Detect FF FE, UTF-16, little-endian`() {
//        val input = ByteArrayInput(
//            byteArrayOf(
//                0xFF.toByte(), 0xFE.toByte(),
//                49.toByte(), 0.toByte()
//            )
//        )
//        val reader = YamlUnicodeReader(input)
//        reader.init()
//        assertEquals(StandardCharsets.UTF_16LE, reader.getEncoding())
//        assertEquals('1', reader.read(), "BOM must be skipped, #49 -> 1")
//        reader.close()
//    }
}
