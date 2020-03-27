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
package uk.org.jurg.yakl.engine.v2.api.lowlevel

import kotlinx.io.ByteArrayInput
import uk.org.jurg.yakl.engine.utils.StringReader
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.events.StreamEndEvent
import uk.org.jurg.yakl.engine.v2.events.StreamStartEvent
import uk.org.jurg.yakl.engine.v2.utils.TestUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseTest {
    @Test
    fun parseEmptyReader() {
        val parse = Parse(LoadSettings())
        val events = parse.parseReader(StringReader("")).toList()
        assertEquals(2, events.size)
        TestUtils.compareEvents(listOf(StreamStartEvent(), StreamEndEvent()), events)
    }

    @Test
    fun parseEmptyInputStream() {
        val parse = Parse(LoadSettings())
        val events =
            parse.parseInputStream(ByteArrayInput(byteArrayOf())).toList()
        assertEquals(2, events.size)
        TestUtils.compareEvents(listOf(StreamStartEvent(), StreamEndEvent()), events)
    }
}
