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
package uk.org.jurg.yakl.engine.v2.api.lowlevel

import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.events.DocumentEndEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentStartEvent
import uk.org.jurg.yakl.engine.v2.events.ImplicitTuple
import uk.org.jurg.yakl.engine.v2.events.ScalarEvent
import uk.org.jurg.yakl.engine.v2.events.StreamEndEvent
import uk.org.jurg.yakl.engine.v2.events.StreamStartEvent
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.utils.TestUtils.compareEvents
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SerializeTest {
    @Test
    fun serializeOneScalar() {
        val serialize = Serialize(DumpSettings())
        val events = serialize.serializeOne(ScalarNode(Tag.STR, value = "a", style = ScalarStyle.PLAIN)).toList()
        assertEquals(5, events.size)
        compareEvents(
            listOf(
                StreamStartEvent(),
                DocumentStartEvent(explicit = false, specVersion = null, tags = HashMap()),
                ScalarEvent(
                    anchor = null,
                    tag = null,
                    implicit = ImplicitTuple(plain = false, nonPlain = false),
                    value = "a",
                    scalarStyle = ScalarStyle.PLAIN
                ),
                DocumentEndEvent(false),
                StreamEndEvent()
            ), events
        )
    }
}
