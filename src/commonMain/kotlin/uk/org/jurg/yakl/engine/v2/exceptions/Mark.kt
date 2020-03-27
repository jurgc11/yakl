/**
 * Copyright (c) 2018, http://www.snakeyaml.org
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.jurg.yakl.engine.v2.exceptions

import uk.org.jurg.yakl.engine.utils.codePointToString
import uk.org.jurg.yakl.engine.v2.common.CharConstants

/**
 * It's just a record and its only use is producing nice error messages. Parser
 * does not use it for any other purposes.
 *
 * @param name    - the name to be used as identifier
 * @param index   - the index from the beginning of the stream
 * @param line    - line of the mark from beginning of the stream
 * @param column  - column of the mark from beginning of the line
 * @param buffer  - the data
 * @param pointer - the position of the mark from the beginning of the stream
 */
class Mark(
    val name: String,
    val index: Int,
    val line: Int,
    val column: Int,
    val buffer: IntArray,
    val pointer: Int) {

    private fun isLineBreak(c: Int): Boolean {
        return CharConstants.NULL_OR_LINEBR.has(c)
    }

    fun createSnippet(indent: Int, maxLength: Int): String {
        val half = maxLength / 2f - 1f
        var start = pointer
        var head = ""
        while (start > 0 && !isLineBreak(buffer[start - 1])) {
            start -= 1
            if (pointer - start > half) {
                head = " ... "
                start += 5
                break
            }
        }
        var tail = ""
        var end = pointer
        while (end < buffer.size && !isLineBreak(buffer[end])) {
            end += 1
            if (end - pointer > half) {
                tail = " ... "
                end -= 5
                break
            }
        }
        val result = StringBuilder()
        result.append(" ".repeat(indent))
        result.append(head)
        for (i in start until end) {
            result.append(buffer[i].codePointToString())
        }
        result.append(tail)
        result.append("\n")
        result.append(" ".repeat(indent + pointer - start + head.length))
        result.append("^")
        return result.toString()
    }

    fun createSnippet(): String {
        return createSnippet(4, 75)
    }

    override fun toString(): String {
        val snippet = createSnippet()
        val builder = StringBuilder(" in ")
        builder.append(name)
        builder.append(", line ")
        builder.append(line + 1)
        builder.append(", column ")
        builder.append(column + 1)
        builder.append(":\n")
        builder.append(snippet)
        return builder.toString()
    }

}
