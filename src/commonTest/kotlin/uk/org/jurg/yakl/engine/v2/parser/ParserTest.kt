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


package uk.org.jurg.yakl.engine.v2.parser


import uk.org.jurg.yakl.engine.utils.StringReader
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.scanner.ScannerImpl
import uk.org.jurg.yakl.engine.v2.scanner.StreamReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ParserTest {
    @Test
    fun `Expected NoSuchElementException after all the events are finished.`() {
        val settings = LoadSettings()
        val reader = StreamReader(StringReader("444333"), settings)
        val scanner = ScannerImpl(reader)
        val parser = ParserImpl(scanner, settings)
        assertTrue(parser.hasNext())
        assertEquals(Event.ID.StreamStart, parser.next().eventId)
        assertTrue(parser.hasNext())
        assertEquals(Event.ID.DocumentStart, parser.next().eventId)
        assertTrue(parser.hasNext())
        assertEquals(Event.ID.Scalar, parser.next().eventId)
        assertTrue(parser.hasNext())
        assertEquals(Event.ID.DocumentEnd, parser.next().eventId)
        assertTrue(parser.hasNext())
        assertEquals(Event.ID.StreamEnd, parser.next().eventId)
        assertFalse(parser.hasNext())
        try {
            parser.next()
            fail("Expected NoSuchElementException")
        } catch (e: NoSuchElementException) {
            assertEquals("No more Events found.", e.message)
        }
    }
}
