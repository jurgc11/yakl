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
package uk.org.jurg.yakl.engine.usecases.inherited

import uk.org.jurg.yakl.engine.utils.charCount
import uk.org.jurg.yakl.engine.utils.codePointAt
import uk.org.jurg.yakl.engine.utils.isSupplementaryCodePoint
import uk.org.jurg.yakl.engine.utils.toIntArray
import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.exceptions.Mark
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.scanner.Scanner
import uk.org.jurg.yakl.engine.v2.tokens.AliasToken
import uk.org.jurg.yakl.engine.v2.tokens.AnchorToken
import uk.org.jurg.yakl.engine.v2.tokens.DirectiveToken
import uk.org.jurg.yakl.engine.v2.tokens.DocumentEndToken
import uk.org.jurg.yakl.engine.v2.tokens.DocumentStartToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowEntryToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowMappingEndToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowMappingStartToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowSequenceEndToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowSequenceStartToken
import uk.org.jurg.yakl.engine.v2.tokens.KeyToken
import uk.org.jurg.yakl.engine.v2.tokens.ScalarToken
import uk.org.jurg.yakl.engine.v2.tokens.StreamEndToken
import uk.org.jurg.yakl.engine.v2.tokens.StreamStartToken
import uk.org.jurg.yakl.engine.v2.tokens.TagToken
import uk.org.jurg.yakl.engine.v2.tokens.TagTuple
import uk.org.jurg.yakl.engine.v2.tokens.Token
import uk.org.jurg.yakl.engine.v2.tokens.ValueToken
import uk.org.jurg.yakl.engine.v2.tokens.YAML_DIRECTIVE

class CanonicalScanner(data: String, private val label: String) : Scanner {
    companion object {
        private val DIRECTIVE = "%YAML 1.2"
        private val ESCAPE_CODES = mapOf(
            'x' to 2,// 8-bit Unicode
            'u' to 4,// 16-bit Unicode
            'U' to 8 // 32-bit Unicode (Supplementary characters are supported)
        )
        val ESCAPE_REPLACEMENTS = mapOf(
            '0' to "\u0000", // ASCII null
            'a' to "\u0007", // ASCII bell
            'b' to "\u0008", // ASCII backspace
            't' to "\u0009", // ASCII horizontal tab
            'n' to "\n", // ASCII newline (line feed; &#92;n maps to 0x0A)
            'v' to "\u000B", // ASCII vertical tab
            'f' to "\u000C", // ASCII form-feed
            'r' to "\r", // carriage-return (&#92;r maps to 0x0D)
            'e' to "\u001B", // ASCII escape character (Esc)
            ' ' to "\u0020", // ASCII space
            '"' to "\"", // ASCII double-quote
            '\\' to "\\", // ASCII backslash
            'N' to "\u0085", // Unicode next line
            '_' to "\u00A0", // Unicode non-breaking-space
            'L' to "\u2028", // Unicode line-separator
            'P' to "\u2029" // Unicode paragraph separator
        )
    }

    private val data = data + "\u0000"
    private var index: Int = 0
    val tokens = mutableListOf<Token>()
    private var scanned = false
    private val mark = Mark("test", 0, 0, 0, data.toIntArray(), 0)

    override fun checkToken(vararg choices: Token.ID): Boolean {
        if (!scanned) {
            scan()
        }
        if (tokens.isNotEmpty()) {
            if (choices.isEmpty()) {
                return true
            }
            val first = tokens[0]
            for (choice in choices) {
                if (first.tokenId === choice) {
                    return true
                }
            }
        }
        return false
    }

    override fun peekToken(): Token {
        if (!scanned) {
            scan()
        }
        return if (tokens.isNotEmpty()) {
            tokens[0]
        } else {
            throw IllegalStateException()
        }
    }

    override fun hasNext(): Boolean {
        return checkToken()
    }

    override fun next(): Token {
        if (!scanned) {
            scan()
        }
        return tokens.removeAt(0)
    }

    fun getToken(choice: Token.ID?): Token? {
        val token = next()
        if (choice != null && token.tokenId !== choice) {
            throw CanonicalException("unexpected token $token")
        }
        return token
    }

    private fun scan() {
        tokens.add(StreamStartToken(mark, mark))
        var stop = false
        while (!stop) {
            findToken()
            when (val c = data[index]) {
                '\u0000' -> {
                    tokens.add(StreamEndToken(mark, mark))
                    stop = true
                }
                '%' -> tokens.add(scanDirective())
                '-' -> if ("---" == data.substring(index, index + 3)) {
                    index += 3
                    tokens.add(DocumentStartToken(mark, mark))
                }
                '.' -> if ("..." == data.substring(index, index + 3)) {
                    index += 3
                    tokens.add(DocumentEndToken(mark, mark))
                }
                '[' -> {
                    index++
                    tokens.add(FlowSequenceStartToken(mark, mark))
                }
                '{' -> {
                    index++
                    tokens.add(FlowMappingStartToken(mark, mark))
                }
                ']' -> {
                    index++
                    tokens.add(FlowSequenceEndToken(mark, mark))
                }
                '}' -> {
                    index++
                    tokens.add(FlowMappingEndToken(mark, mark))
                }
                '?' -> {
                    index++
                    tokens.add(KeyToken(mark, mark))
                }
                ':' -> {
                    index++
                    tokens.add(ValueToken(mark, mark))
                }
                ',' -> {
                    index++
                    tokens.add(FlowEntryToken(mark, mark))
                }
                '*' -> tokens.add(scanAlias())
                '&' -> tokens.add(scanAlias())
                '!' -> tokens.add(scanTag())
                '"' -> tokens.add(scanScalar())
                else -> throw CanonicalException("invalid token: $c in $label")
            }
        }
        scanned = true
    }

    private fun scanDirective(): Token {
        val chunk1 = data.substring(index, index + DIRECTIVE.length)
        val chunk2 = data[index + DIRECTIVE.length]
        return if (DIRECTIVE == chunk1 && "\n\u0000".contains(chunk2)) {
            index += DIRECTIVE.length
            DirectiveToken(YAML_DIRECTIVE, listOf(1, 1), mark, mark)
        } else {
            throw CanonicalException("invalid directive: $chunk1 $chunk2 in $label")
        }
    }

    private fun scanAlias(): Token {
        val c: Int = data.codePointAt(index)
        val isTokenClassAlias = c == '*'.toInt()
        index += c.charCount()
        val start = index
        while (!", \n\u0000".contains(data[index])) {
            index++
        }
        val value = data.substring(start, index)

        return if (isTokenClassAlias) {
            AliasToken(Anchor(value), mark, mark)
        } else {
            AnchorToken(Anchor(value), mark, mark)
        }
    }

    private fun scanTag(): Token {
        index += data.codePointAt(index).charCount()
        val start = index
        while (!" \n\u0000".contains(data[index])) {
            index++
        }
        val value = data.substring(start, index)
        val value2 = when {
            value.isEmpty() -> "!"
            value[0] == '!' -> Tag.PREFIX + value.substring(1)
            value[0] == '<' && value[value.length - 1] == '>' -> value.substring(1, value.length - 1)
            else -> "!$value"
        }
        return TagToken(TagTuple("", value2), mark, mark)
    }

    private fun scanScalar(): Token {
        index += data.codePointAt(index).charCount()
        val chunks = StringBuilder()
        var start = index
        var ignoreSpaces = false
        while (data[index] != '"') {
            if (data[index] == '\\') {
                ignoreSpaces = false
                chunks.append(data, start, index)
                index += data.codePointAt(index).charCount()
                val c = data.codePointAt(index)
                index += data.codePointAt(index).charCount()
                if (c == '\n'.toInt()) {
                    ignoreSpaces = true
                } else if (!c.isSupplementaryCodePoint() && ESCAPE_CODES.containsKey(c.toChar())) {
                    val length = ESCAPE_CODES.getValue(c.toChar())
                    val code = data.substring(index, index + length).toInt(16)
                    chunks.append(code.toChar())
                    index += length
                } else {
                    if (c.isSupplementaryCodePoint() || !ESCAPE_REPLACEMENTS.containsKey(c.toChar())) {
                        throw CanonicalException("invalid escape code")
                    }
                    chunks.append(ESCAPE_REPLACEMENTS[c.toChar()])
                }
                start = index
            } else if (data[index] == '\n') {
                chunks.append(data, start, index)
                chunks.append(" ")
                index += data.codePointAt(index).charCount()
                start = index
                ignoreSpaces = true
            } else if (ignoreSpaces && data[index] == ' ') {
                index += data.codePointAt(index).charCount()
                start = index
            } else {
                ignoreSpaces = false
                index += data.codePointAt(index).charCount()
            }
        }
        chunks.append(data, start, index)
        index += data.codePointAt(index).charCount()
        return ScalarToken(chunks.toString(), false, startMark = mark, endMark = mark)
    }

    private fun findToken() {
        var found = false
        while (!found) {
            while (" \t".contains(data[index])) {
                index++
            }
            if (data[index] == '#') {
                while (data[index] != '\n') {
                    index++
                }
            }
            if (data[index] == '\n') {
                index++
            } else {
                found = true
            }
        }
    }


}
