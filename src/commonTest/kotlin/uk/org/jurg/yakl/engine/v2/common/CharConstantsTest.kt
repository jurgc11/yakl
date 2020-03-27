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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharConstantsTest {
    @Test
    //http://www.yaml.org/spec/1.2/spec.html#id2774608
    fun `LINEBR contains only LF and CR`() {
        assertTrue(CharConstants.LINEBR.has('\n'.toInt()), "LF must be included")
        assertTrue(CharConstants.LINEBR.has('\r'.toInt()), "CR must not be included")
        assertTrue(CharConstants.LINEBR.hasNo('\u0085'.toInt()), "85 (next line) must not be included in 1.2")
        assertTrue(CharConstants.LINEBR.hasNo('\u2028'.toInt()), "2028 (line separator) must not be included in 1.2")
        assertTrue(
            CharConstants.LINEBR.hasNo('\u2029'.toInt()),
            "2029 (paragraph separator) must not be included in 1.2"
        )
        assertTrue(CharConstants.LINEBR.hasNo('a'.toInt()), "normal char should not be included")
    }

    @Test
    fun `NULL_OR_LINEBR contains 3 chars`() {
        assertTrue(CharConstants.NULL_OR_LINEBR.has('\n'.toInt()))
        assertTrue(CharConstants.NULL_OR_LINEBR.has('\r'.toInt()))
        assertTrue(CharConstants.NULL_OR_LINEBR.has('\u0000'.toInt()))
        assertFalse(CharConstants.NULL_OR_LINEBR.has('\u0085'.toInt()), "85 (next line) must not be included in 1.2")
        assertFalse(
            CharConstants.NULL_OR_LINEBR.has('\u2028'.toInt()),
            "2028 (line separator) must not be included in 1.2"
        )
        assertFalse(
            CharConstants.NULL_OR_LINEBR.has('\u2029'.toInt()),
            "2029 (paragraph separator) must not be included in 1.2"
        )
        assertFalse(CharConstants.NULL_OR_LINEBR.has('b'.toInt()), "normal char should not be included")
    }

    @Test
    fun `additional chars`() {
        assertTrue(CharConstants.NULL_BL_LINEBR.hasNo('1'.toInt()))
        assertTrue(CharConstants.NULL_BL_LINEBR.has('1'.toInt(), "123"))
        assertTrue(CharConstants.NULL_BL_LINEBR.hasNo('4'.toInt(), "123"))
    }

    @Test
    fun `ESCAPE_REPLACEMENTS`() {
        assertEquals(97, 'a'.toInt())
        assertEquals(17, CharConstants.ESCAPE_REPLACEMENTS.size)
        assertEquals(15, CharConstants.ESCAPES.size)
        assertEquals('\r', CharConstants.ESCAPE_REPLACEMENTS[114])
    }
}
