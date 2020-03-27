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

package uk.org.jurg.yakl.engine.v2.common

private const val ASCII_SIZE = 128

private const val ALPHA_S = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-_"
private const val LINEBR_S = "\n\r"
private const val NULL_OR_LINEBR_S = "\u0000$LINEBR_S"
private const val NULL_BL_LINEBR_S = " $NULL_OR_LINEBR_S"
private const val NULL_BL_T_LINEBR_S = "\t$NULL_BL_LINEBR_S"
private const val NULL_BL_T_S = "\u0000 \t"
private const val URI_CHARS_S = "$ALPHA_S-;/?:@&=+$,_.!~*\'()[]%"

class CharConstants(content: String) {
    val contains = Array(ASCII_SIZE) { false }

    init {
        content.map { contains[it.toInt()] = true }
    }

    fun has(c: Int): Boolean {
        return c < ASCII_SIZE && contains[c]
    }

    fun hasNo(c: Int): Boolean {
        return !has(c)
    }

    fun has(c: Int, additional: String): Boolean {
        return has(c) || additional.indexOf(c.toChar()) != -1
    }

    fun hasNo(c: Int, additional: String): Boolean {
        return !has(c, additional)
    }

    companion object {

        val LINEBR = CharConstants(LINEBR_S)
        val NULL_OR_LINEBR = CharConstants(NULL_OR_LINEBR_S)
        val NULL_BL_LINEBR = CharConstants(NULL_BL_LINEBR_S)
        val NULL_BL_T_LINEBR = CharConstants(NULL_BL_T_LINEBR_S)
        val NULL_BL_T = CharConstants(NULL_BL_T_S)
        val URI_CHARS = CharConstants(URI_CHARS_S)
        val ALPHA = CharConstants(ALPHA_S)

        /**
         * A mapping from an escaped character in the input stream to the character
         * that they should be replaced with.
         *
         *
         * YAML defines several common and a few uncommon escape sequences.
         */
        val ESCAPE_REPLACEMENTS: Map<Int, Char> = mapOf(
            '0'.toInt() to '\u0000', // ASCII null
            'a'.toInt() to '\u0007', // ASCII bell
            'b'.toInt() to '\u0008',  // ASCII backspace
            't'.toInt() to '\u0009',  // ASCII horizontal tab
            'n'.toInt() to '\n', // ASCII newline (line feed; &#92;n maps to 0x0A)
            'v'.toInt() to '\u000B', // ASCII vertical tab
            'f'.toInt() to '\u000C', // ASCII form-feed
            'r'.toInt() to '\r', // carriage-return (&#92;r maps to 0x0D)
            'e'.toInt() to '\u001B', // ASCII escape character (Esc)
            ' '.toInt() to '\u0020',// ASCII, space
            '"'.toInt() to '\"', // ASCII double-quote
            '/'.toInt() to '/', // ASCII slash (#x2F), for JSON compatibility.
            '\\'.toInt() to '\\', // ASCII backslash
            'N'.toInt() to '\u0085', // Unicode next line
            '_'.toInt() to '\u00A0', // Unicode non-breaking-space
            'L'.toInt() to '\u2028', // Unicode line-separator
            'P'.toInt() to '\u2029' // Unicode paragraph separator
        )

        /**
         * A mapping from a character to be escaped to its code in the output stream. (used for emitting)
         * It contains the same as ESCAPE_REPLACEMENTS except ' ' and '/'
         *
         *
         * YAML defines several common and a few uncommon escape sequences.
         *
         * @see [5.7. Escaped Characters](http://www.yaml.org/spec/1.2/spec.html.id2776092)
         */
        val ESCAPES: Map<Char, Int> = ESCAPE_REPLACEMENTS.asSequence()
            .filter { it.key.toChar() != ' ' && it.key.toChar() != '/' }
            .associateBy({ it.value }, { it.key })

        /**
         * A mapping from a character to a number of bytes to read-ahead for that
         * escape sequence. These escape sequences are used to handle unicode
         * escaping in the following formats, where H is a hexadecimal character:
         * <pre>
         * &#92;xHH         : escaped 8-bit Unicode character
         * &#92;uHHHH       : escaped 16-bit Unicode character
         * &#92;UHHHHHHHH   : escaped 32-bit Unicode character
        </pre> *
         */
        val ESCAPE_CODES: Map<Char, Int> = mapOf(
            'x' to 2, // 8-bit Unicode
            'u' to 4, // 16-bit Unicode
            'U' to 8 // 32-bit Unicode (Supplementary characters are supported)
        )
    }

}
