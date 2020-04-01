/*
 * Copyright (c) 2008 Google Inc.
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


object UriEncoder {

    @OptIn(ExperimentalStdlibApi::class)
    private val UPPER_HEX_DIGITS = "0123456789ABCDEF".toCharArray()
    private val URI_ESCAPED_SPACE = charArrayOf('+')

    /**
     * The amount of padding (chars) to use when growing the escape buffer.
     */
    private const val DEST_PAD = 32
    private const val plusForSpace = false
    private const val safeChars = "-_.!~*'()@:$&,;=[]/"
    private val safeOctets = BooleanArray(127)
    init {
        for (c in '0'..'9') {
            safeOctets[c.toInt()] = true
        }
        for (c in 'A'..'Z') {
            safeOctets[c.toInt()] = true
        }
        for (c in 'a'..'z') {
            safeOctets[c.toInt()] = true
        }
        for (c in safeChars) {
            safeOctets[c.toInt()] = true
        }
    }

    /**
     * Escape special characters with '%'
     *
     * @param uri URI to be escaped
     * @return encoded URI
     */
    fun encode(s: String): String {
        s.forEachIndexed { index, c ->
            if (c.toInt() >= safeOctets.size || !safeOctets[c.toInt()]) {
                return escapeSlow(s, index)
            }
        }

        return s
    }

    private fun escapeSlow(s: String, index1: Int): String {
        val end = s.length

        // Get a destination buffer and setup some loop variables.

        // Get a destination buffer and setup some loop variables.
        var dest = CharArray(1024)
        var destIndex = 0
        var unescapedChunkStart = 0
        var index = index1

        while (index < end) {
            val cp = s.codePointAt(index)
            require(cp >= 0) { "Trailing high surrogate at end of input" }
            val escaped = escape(cp)
            if (escaped != null) {
                val charsSkipped = index - unescapedChunkStart

                // This is the size needed to add the replacement, not the full
                // size needed by the string. We only regrow when we absolutely
                // must.
                val sizeNeeded = destIndex + charsSkipped + escaped.size
                if (dest.size < sizeNeeded) {
                    val destLength = sizeNeeded + (end - index) + DEST_PAD
                    dest =
                        growBuffer(dest, destIndex, destLength)
                }
                // If we have skipped any characters, we need to copy them now.
                if (charsSkipped > 0) {
                    s.copyChars(dest, destIndex, unescapedChunkStart, index)
                    destIndex += charsSkipped
                }
                if (escaped.isNotEmpty()) {
                    escaped.copyInto(dest, destIndex, 0, escaped.size)
                    destIndex += escaped.size
                }
            }
            unescapedChunkStart = index + if (cp.isSupplementaryCodePoint()) 2 else 1
            index =
                nextEscapeIndex(s, unescapedChunkStart, end)
        }

        // Process trailing unescaped characters - no need to account for
        // escaped
        // length or padding the allocation.

        // Process trailing unescaped characters - no need to account for
        // escaped
        // length or padding the allocation.
        val charsSkipped = end - unescapedChunkStart
        if (charsSkipped > 0) {
            val endIndex = destIndex + charsSkipped
            if (dest.size < endIndex) {
                dest = growBuffer(dest, destIndex, endIndex)
            }
            s.copyChars(dest, destIndex, unescapedChunkStart, end)
            destIndex = endIndex
        }
        return String(dest, 0, destIndex)
    }

    private fun String.copyChars(dest: CharArray, destOffset: Int, srcStart: Int, srcEnd: Int) {
        this.subSequence(srcStart, srcEnd).forEachIndexed { index, char ->
            dest[destOffset + index] = char
        }
    }

    private fun nextEscapeIndex(csq: String, start: Int, end: Int): Int {
        var index = start
        while (index < end) {
            val cp: Int = csq.codePointAt(index)

            if (cp < 0 || escape(cp) != null) {
                break
            }
            index += if (cp.isSupplementaryCodePoint()) 2 else 1
        }
        return index
    }

    private fun escape(cp1: Int): CharArray? {
        // We should never get negative values here but if we do it will throw
        // an
        // IndexOutOfBoundsException, so at least it will get spotted.
        var cp = cp1
        return when {
            cp < safeOctets.size && safeOctets[cp] -> {
                null
            }
            cp == ' '.toInt() && plusForSpace -> {
                URI_ESCAPED_SPACE
            }
            cp <= 0x7F -> {
                // Single byte UTF-8 characters
                // Start with "%--" and fill in the blanks
                val dest = CharArray(3)
                dest[0] = '%'
                dest[2] = UPPER_HEX_DIGITS[cp and 0xF]
                dest[1] = UPPER_HEX_DIGITS[cp ushr 4]
                dest
            }
            cp <= 0x7ff -> {
                // Two byte UTF-8 characters [cp >= 0x80 && cp <= 0x7ff]
                // Start with "%--%--" and fill in the blanks
                val dest = CharArray(6)
                dest[0] = '%'
                dest[3] = '%'
                dest[5] = UPPER_HEX_DIGITS[cp and 0xF]
                cp = cp ushr 4
                dest[4] = UPPER_HEX_DIGITS[0x8 or (cp and 0x3)]
                cp = cp ushr 2
                dest[2] = UPPER_HEX_DIGITS[cp and 0xF]
                cp = cp ushr 4
                dest[1] = UPPER_HEX_DIGITS[0xC or cp]
                dest
            }
            cp <= 0xffff -> {
                // Three byte UTF-8 characters [cp >= 0x800 && cp <= 0xffff]
                // Start with "%E-%--%--" and fill in the blanks
                val dest = CharArray(9)
                dest[0] = '%'
                dest[1] = 'E'
                dest[3] = '%'
                dest[6] = '%'
                dest[8] = UPPER_HEX_DIGITS[cp and 0xF]
                cp = cp ushr 4
                dest[7] = UPPER_HEX_DIGITS[0x8 or (cp and 0x3)]
                cp = cp ushr 2
                dest[5] = UPPER_HEX_DIGITS[cp and 0xF]
                cp = cp ushr 4
                dest[4] = UPPER_HEX_DIGITS[0x8 or (cp and 0x3)]
                cp = cp ushr 2
                dest[2] = UPPER_HEX_DIGITS[cp]
                dest
            }
            cp <= 0x10ffff -> {
                val dest = CharArray(12)
                // Four byte UTF-8 characters [cp >= 0xffff && cp <= 0x10ffff]
                // Start with "%F-%--%--%--" and fill in the blanks
                dest[0] = '%'
                dest[1] = 'F'
                dest[3] = '%'
                dest[6] = '%'
                dest[9] = '%'
                dest[11] = UPPER_HEX_DIGITS[cp and 0xF]
                cp = cp ushr 4
                dest[10] = UPPER_HEX_DIGITS[0x8 or (cp and 0x3)]
                cp = cp ushr 2
                dest[8] = UPPER_HEX_DIGITS[cp and 0xF]
                cp = cp ushr 4
                dest[7] = UPPER_HEX_DIGITS[0x8 or (cp and 0x3)]
                cp = cp ushr 2
                dest[5] = UPPER_HEX_DIGITS[cp and 0xF]
                cp = cp ushr 4
                dest[4] = UPPER_HEX_DIGITS[0x8 or (cp and 0x3)]
                cp = cp ushr 2
                dest[2] = UPPER_HEX_DIGITS[cp and 0x7]
                dest
            }
            else -> {
                // If this ever happens it is due to bug in UnicodeEscaper, not bad
                // input.
                throw IllegalArgumentException("Invalid unicode character value $cp")
            }
        }
    }

    private fun growBuffer(dest: CharArray, index: Int, size: Int): CharArray {
        val copy = CharArray(size)
        if (index > 0) {
            dest.copyInto(copy)
        }
        return copy
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun decode(s: String): String {
        var needToChange = false
        val numChars = s.length
        val sb = StringBuilder(if (numChars > 500) numChars / 2 else numChars)
        var i = 0
        var c: Char
        var bytes: ByteArray? = null
        while (i < numChars) {
            c = s[i]
            when (c) {
                '+' -> {
                    sb.append(' ')
                    i++
                    needToChange = true
                }
                '%' -> {
                    /*
                 * Starting with this instance of %, process all
                 * consecutive substrings of the form %xy. Each
                 * substring %xy will yield a byte. Convert all
                 * consecutive  bytes obtained this way to whatever
                 * character(s) they represent in the provided
                 * encoding.
                 */try {

                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        if (bytes == null) bytes = ByteArray((numChars - i) / 3)
                        var pos = 0
                        while (i + 2 < numChars &&
                            c == '%'
                        ) {
                            val v = s.substring(i+1, i+3).toInt(16)
                            require(v >= 0) {
                                ("URLDecoder: Illegal hex characters in escape "
                                        + "(%) pattern - negative value")
                            }
                            bytes[pos++] = v.toByte()
                            i += 3
                            if (i < numChars) c = s[i]
                        }

                        // A trailing, incomplete byte encoding such as
                        // "%x" will cause an exception to be thrown
                        require(!(i < numChars && c == '%')) { "URLDecoder: Incomplete trailing escape (%) pattern" }
                        sb.append(bytes.decodeToString(0, pos))
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException(
                            "URLDecoder: Illegal hex characters in escape (%) pattern - "
                                    + e.message
                        )
                    }
                    needToChange = true
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return if (needToChange) sb.toString() else s
    }
}
