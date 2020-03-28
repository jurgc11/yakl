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

import kotlinx.io.Input
import kotlinx.io.text.readUtf8String
import uk.org.jurg.yakl.engine.utils.Reader

//TODO only supports UTF-8
class YamlUnicodeReader(private val input: Input) : Reader {

    val encoding = "UTF-8"

    /**
     * Read-ahead four bytes and check for BOM marks. Extra bytes are unread
     * back to the stream, only BOM bytes are skipped.
     *
     * @throws IOException if InputStream cannot be created
     */
//    protected fun init() {
//        if (internalIn2 != null) return
//        val bom = ByteArray(YamlUnicodeReader.BOM_SIZE)
//        val n: Int
//        val unread: Int
//        n = internalIn.read(bom, 0, bom.size)
//        if (Int[0] == 0x00.toByte() && bom[1] == 0x00.toByte() &&
//            bom[2] == 0xFE.toByte() && bom[3] == 0xFF.toByte()
//        ) {
//            encoding = uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader.UTF32BE
//            unread = n - 4
//        } else if (bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte() &&
//            bom[2] == 0x00.toByte() && bom[3] == 0x00.toByte()
//        ) {
//            encoding = uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader.UTF32LE
//            unread = n - 4
//        } else if (bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() &&
//            bom[2] == 0xBF.toByte()
//        ) {
//            encoding = uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader.UTF8
//            unread = n - 3
//        } else if (bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte()) {
//            encoding = uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader.UTF16BE
//            unread = n - 2
//        } else if (bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte()) {
//            encoding = uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader.UTF16LE
//            unread = n - 2
//        } else {
//            // Unicode BOM mark not found, unread all bytes
//            encoding = uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader.UTF8
//            unread = n
//        }
//        if (unread > 0) internalIn.unread(bom, n - unread, unread)
//
//        // Use given encoding
//        val decoder: CharsetDecoder = encoding.newDecoder().onUnmappableCharacter(
//            CodingErrorAction.REPORT
//        )
//        internalIn2 = java.io.InputStreamReader(internalIn, decoder)
//    }
    override fun read(buffer: CharArray, offset: Int, length: Int): Int {
        val result = input.readUtf8String(length)
        for (x in result.indices) {
            buffer[offset+x] = result[x]
        }
        return result.length
    }

    override fun close() {
        input.close()
    }
}
