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
package uk.org.jurg.yakl.engine.usecases.binary

import uk.org.jurg.yakl.engine.v2.api.Dump
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Serialize
import uk.org.jurg.yakl.engine.v2.common.NonPrintableStyle
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.events.ScalarEvent
import uk.org.jurg.yakl.engine.v2.nodes.NodeType
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.representer.StandardRepresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BinaryRoundTripTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testBinary() {
        val dumper = Dump(
            DumpSettings(
                nonPrintableStyle = NonPrintableStyle.BINARY
            )
        )
        val source = "\u0096"
        val serialized = dumper.dumpToString(source)
        assertEquals(
            """!!binary |-
  wpY=
""", serialized
        )
        //parse back to bytes
        val loader = Load(LoadSettings())
        val deserialized = loader.loadFromString(serialized) as ByteArray
        assertEquals(source, deserialized.decodeToString())
    }

    @Test
    fun testBinaryNode() {
        val source = "\u0096"
        val standardRepresenter = StandardRepresenter(
            DumpSettings(
                nonPrintableStyle = NonPrintableStyle.BINARY
            )
        )
        val scalar = standardRepresenter.represent(source) as ScalarNode
        //check Node
        assertEquals(Tag.BINARY, scalar.tag)
        assertEquals(NodeType.SCALAR, scalar.nodeType)
        assertEquals("wpY=", scalar.value)
        //check Event
        val serialize = Serialize(DumpSettings())
        val events = serialize.serializeOne(scalar)
        assertEquals(5, events.size)
        val data = events[2] as ScalarEvent
        assertEquals(Tag.BINARY.toString(), data.tag)
        assertEquals(ScalarStyle.LITERAL, data.scalarStyle)
        assertEquals("wpY=", data.value)
        val implicit = data.implicit
        assertFalse(implicit.canOmitTagInPlainScalar())
        assertFalse(implicit.canOmitTagInNonPlainScalar())
    }

    @Test
    fun testStrNode() {
        val standardRepresenter = StandardRepresenter(DumpSettings())
        val source = "\u0096"
        val scalar = standardRepresenter.represent(source) as ScalarNode
        val node = standardRepresenter.represent(source)
        assertEquals(Tag.STR, node.tag)
        assertEquals(NodeType.SCALAR, node.nodeType)
        assertEquals("\u0096", scalar.value)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testRoundTripBinary() {
        val dumper = Dump(
            DumpSettings(
                nonPrintableStyle = NonPrintableStyle.ESCAPE
            )
        )
        val toSerialized = mapOf("key" to "a\u0096b")
        val output = dumper.dumpToString(toSerialized)
        assertEquals("{key: \"a\\x96b\"}\n", output)
        val loader = Load(LoadSettings())
        val parsed = loader.loadFromString(output) as Map<String, String>
        assertEquals(toSerialized["key"], parsed["key"])
        assertEquals(toSerialized, parsed)
    }
}
