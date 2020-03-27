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
package uk.org.jurg.yakl.engine.v2.scanner

import kotlinx.io.IOException
import uk.org.jurg.yakl.engine.utils.Reader
import uk.org.jurg.yakl.engine.utils.charCount
import uk.org.jurg.yakl.engine.utils.codePointAt
import uk.org.jurg.yakl.engine.utils.isPrintable
import uk.org.jurg.yakl.engine.utils.toString
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.common.CharConstants
import uk.org.jurg.yakl.engine.v2.exceptions.Mark
import uk.org.jurg.yakl.engine.v2.exceptions.ReaderException
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import kotlin.math.min


/**
 * Reader: checks if code points are in allowed range. Returns '\0' when end of
 * data has been reached.
 */
class StreamReader(val stream: Reader, private val loadSettings: LoadSettings) {

    /**
     * Read data (as a moving window for input stream)
     */
    private var dataWindow = IntArray(0)

    /**
     * Real length of the data in dataWindow
     */
    private var dataLength = 0

    /**
     * The variable points to the current position in the data array
     */
    private var pointer = 0
    private var eof = false

    /**
     * @return current position as number (in characters) from the beginning of the stream
     */
    /**
     * index is only required to implement 1024 key length restriction
     * It must count code points, but it counts characters (to be fixed)
     */
    private var index = 0 // in code points

    private var line = 0

    //in code points
    private var column = 0

    // temp buffer for one read operation (to avoid creating the array in stack)
    private val buffer = CharArray(loadSettings.bufferSize)

    val mark: Mark?
        get() = if (loadSettings.useMarks) {
            Mark(loadSettings.label, index, line, column, dataWindow, pointer)
        } else  {
            null
        }

    /**
     * read the next length characters and move the pointer.
     * if the last character is high surrogate one more character will be read
     *
     * @param length amount of characters to move forward
     */
    fun forward(length: Int = 1) {
        var i = 0
        while (i < length && ensureEnoughData()) {
            val c = dataWindow[pointer++]
            index++
            if (CharConstants.LINEBR.has(c)
                || c == '\r'.toInt() && ensureEnoughData() && dataWindow[pointer] != '\n'.toInt()
            ) {
                line++
                column = 0
            } else if (c != 0xFEFF) {
                column++
            }
            i++
        }
    }

    /**
     * Peek the next index-th code point
     *
     * @param index to peek
     * @return the next index-th code point
     */
    fun peek(index: Int = 0): Int {
        return if (ensureEnoughData(index)) {
            dataWindow[pointer + index]
        } else {
            '\u0000'.toInt()
        }
    }

    /**
     * peek the next length code points
     *
     * @param length amount of the characters to peek
     * @return the next length code points
     */
    fun prefix(length: Int): String {
        return when {
            length == 0 -> ""
            ensureEnoughData(length) -> dataWindow.toString(pointer, length)
            else -> dataWindow.toString(pointer, min(length, dataLength - pointer))
        }
    }

    /**
     * prefix(length) immediately followed by forward(length)
     *
     * @param length amount of characters to get
     * @return the next length code points
     */
    fun prefixForward(length: Int): String {
        val prefix = prefix(length)
        pointer += length
        index += length
        // prefix never contains new line characters
        column += length
        return prefix
    }

    private fun ensureEnoughData(size: Int = 0): Boolean {
        if (!eof && pointer + size >= dataLength) {
            update()
        }
        return pointer + size < dataLength
    }

    private fun copyOfRange(original: IntArray, from: Int, to: Int): IntArray {
        val newLength = to - from
        require(newLength >= 0) { "$from > $to" }
        val copy = IntArray(newLength)

        original.copyInto(copy, 0, 0, min(original.size - from, newLength))

        return copy
    }

    private fun update() {
        try {
            var read = stream.read(buffer, 0, loadSettings.bufferSize - 1)
            if (read > 0) {
                var cpIndex = dataLength - pointer
                dataWindow = copyOfRange(dataWindow, pointer, dataLength + read)
                if (buffer[read - 1].isHighSurrogate()) {
                    if (stream.read(buffer, read, 1) == -1) {
                        eof = true
                    } else {
                        read++
                    }
                }
                var nonPrintable = ' '.toInt()
                var i = 0
                while (i < read) {
                    val codePoint = buffer.codePointAt(i)
                    dataWindow[cpIndex] = codePoint
                    if (codePoint.isPrintable()) {
                        i += codePoint.charCount()
                    } else {
                        nonPrintable = codePoint
                        i = read
                    }
                    cpIndex++
                }
                dataLength = cpIndex
                pointer = 0
                if (nonPrintable != ' '.toInt()) {
                    throw ReaderException(loadSettings.label, cpIndex - 1, nonPrintable, "special characters are not allowed")
                }
            } else {
                eof = true
            }
        } catch (ioe: IOException) {
            throw YamlEngineException(ioe)
        }
    }

    fun getColumn(): Int {
        return column
    }

    /**
     * @return current position as number (in characters) from the beginning of the stream
     */
    fun getIndex(): Int {
        return index
    }

    fun getLine(): Int {
        return line
    }
}
