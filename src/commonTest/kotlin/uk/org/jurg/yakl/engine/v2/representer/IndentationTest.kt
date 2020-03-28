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
package uk.org.jurg.yakl.engine.v2.representer

import uk.org.jurg.yakl.engine.v2.api.Dump
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for issue https://bitbucket.org/asomov/snakeyaml-engine/issues/9/indentation-before-sequence
 */
class IndentationTest {

    private val map = mapOf(
        "key1" to listOf("value1", "value2"),
        "key2" to listOf("value3", "value4")
    )

    private val sequence = listOf(
        mapOf(
            "key1" to "value1",
            "key2" to "value2"
        ),
        mapOf(
            "key3" to "value3",
            "key4" to "value4"
        )
    )

    private fun createDump(indicatorIndent: Int): Dump {
        val settings = DumpSettings(
            defaultFlowStyle = FlowStyle.BLOCK,
            indicatorIndent = indicatorIndent,
            indent = indicatorIndent + 2
        )
        return Dump(settings)
    }

    @Test
    fun `Dump block map seq with default indent settings`() {
        val dump = createDump(0)
        val output = dump.dumpToString(map)
        val expected = """
                |key1:
                |- value1
                |- value2
                |key2:
                |- value3
                |- value4
                |""".trimMargin()
        assertEquals(expected, output)
    }

    @Test
    fun `Dump block seq map with default indent settings`() {
        val dump = createDump(0)
        val output = dump.dumpToString(sequence)
        val expected = """
            |- key1: value1
            |  key2: value2
            |- key3: value3
            |  key4: value4
            |""".trimMargin()
        assertEquals(expected, output)
    }

    @Test
    fun `Dump block seq map with specified indicator indent`() {
        val dump = createDump(2)
        val output = dump.dumpToString(map)
        val expected = """key1:
  - value1
  - value2
key2:
  - value3
  - value4
"""
        assertEquals(expected, output)
    }

    @Test
    fun `Dump block seq map with indicatorIndent=2`() {
        val dump = createDump(2)
        val output = dump.dumpToString(sequence)
        val expected = """  - key1: value1
    key2: value2
  - key3: value3
    key4: value4
"""
        assertEquals(expected, output)
    }
}
