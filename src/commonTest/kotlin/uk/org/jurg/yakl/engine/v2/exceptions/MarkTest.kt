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


package uk.org.jurg.yakl.engine.v2.exceptions

import uk.org.jurg.yakl.engine.utils.toIntArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkTest {

    @Test
    fun `Mark snippet`() {
        var mark = Mark(
            "test1",
            0,
            0,
            0,
            "*The first line.\nThe last line.".toIntArray(),
            0
        )
        assertEquals("    *The first line.\n    ^", mark.createSnippet())
        mark = Mark(
            "test1",
            0,
            0,
            0,
            "The first*line.\nThe last line.".toIntArray(),
            9
        )
        assertEquals("    The first*line.\n             ^", mark.createSnippet())
    }

    @Test
    fun `Mark toString()`() {
        val mark = Mark(
            "test1",
            0,
            0,
            0,
            "*The first line.\nThe last line.".toIntArray(),
            0
        )
        val lines = mark.toString().split("\n")
        assertEquals(" in test1, line 1, column 1:", lines[0])
        assertEquals("*The first line.", lines[1].trim())
        assertEquals("^", lines[2].trim())
    }

    @Test
    fun `Mark position`() {
        val mark = Mark(
            "test1",
            17,
            29,
            213,
            "*The first line.\nThe last line.".toIntArray(),
            0
        )
        assertEquals(17, mark.index, "index is used in JRuby")
        assertEquals(29, mark.line)
        assertEquals(213, mark.column)
    }

    @Test
    fun `Mark buffer`() {
        val mark = Mark(
            "test1",
            0,
            29,
            213,
            "*The first line.\nThe last line.".toIntArray(),
            0
        )
        val buffer = intArrayOf(
            42,
            84,
            104,
            101,
            32,
            102,
            105,
            114,
            115,
            116,
            32,
            108,
            105,
            110,
            101,
            46,
            10,
            84,
            104,
            101,
            32,
            108,
            97,
            115,
            116,
            32,
            108,
            105,
            110,
            101,
            46
        )
        assertEquals(buffer.size, mark.buffer.size)
        var match = true
        for (i in buffer.indices) {
            if (buffer[i] != mark.buffer[i]) {
                match = false
                break
            }
        }
        assertTrue(match)
    }

    @Test
    fun `Mark pointer`() {
        val mark = Mark(
            "test1",
            0,
            29,
            213,
            "*The first line.\nThe last line.".toIntArray(),
            5
        )
        assertEquals(5, mark.pointer)
        assertEquals("test1", mark.name)
    }

    @Test
    fun `Mark: createSnippet(): longer content must be reduced`() {
        val mark = Mark(
            "test1",
            200,
            2,
            36,
            "*The first line,\nThe second line.\nThe third line, which aaaa bbbb ccccc dddddd * contains mor12345678901234\nThe last line.".toIntArray(),
            78
        )
        assertEquals(
            "   ... aaaa bbbb ccccc dddddd * contains mor1234567 ... \n                             ^",
            mark.createSnippet(2, 55)
        )
    }
}
