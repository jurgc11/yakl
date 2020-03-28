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
package uk.org.jurg.yakl.engine.v2.api

import kotlinx.io.ByteArrayInput
import uk.org.jurg.yakl.engine.utils.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


class LoadTest {
    @Test
    fun `String 'a' is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val str = load.loadFromString("a") as String?
        assertEquals("a", str)
    }

    @Test
    fun `Integer 1 is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val integer = load.loadFromString("1") as Int
        assertEquals(1, integer)
    }

    @Test
    fun `Boolean true is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        assertTrue(load.loadFromString("true") as Boolean)
    }

    @Test
    fun `null is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        assertNull(load.loadFromString(""))
    }

    @Test
    fun `null tag is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        assertNull(load.loadFromString("!!null"))
    }

    @Test
    fun `Float is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val doubleValue = load.loadFromString("1.01") as Double
        assertEquals(1.01, doubleValue)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `Load from InputStream`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val v = load.loadFromInputStream(ByteArrayInput("aaa".encodeToByteArray())) as String
        assertEquals("aaa", v)
    }

    @Test
    fun `Load from Reader`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val v = load.loadFromReader(StringReader("bbb")) as String
        assertEquals("bbb", v)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `Load all from Input`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val input = ByteArrayInput("bbb\n---\nccc\n---\nddd".encodeToByteArray())
        val v = load.loadAllFromInputStream(input)
        val iter = v.iterator()
        assertTrue(iter.hasNext())
        val o1 = iter.next()
        assertEquals("bbb", o1)
        assertTrue(iter.hasNext())
        val o2 = iter.next()
        assertEquals("ccc", o2)
        assertTrue(iter.hasNext())
        val o3 = iter.next()
        assertEquals("ddd", o3)
        assertFalse(iter.hasNext())
    }

    @Test
    fun `Load all from String`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val v = load.loadAllFromString("1\n---\n2\n---\n3")
        var counter = 1
        for (o in v) {
            assertEquals(counter++, o)
        }
    }

    @Test
    fun `Load all from Reader`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val v = load.loadAllFromReader(StringReader("bbb"))
        val iter = v.iterator()
        assertTrue(iter.hasNext())
        val o1 = iter.next()
        assertEquals("bbb", o1)
        assertFalse(iter.hasNext())
    }

    @Test
    fun `Load a lot of documents from the same Load instance (not recommended)`() {
        val settings = LoadSettings()
        val load = Load(settings)
        for (i in 0..9999) {
            val v = load.loadAllFromReader(StringReader("{foo: bar, list: [1, 2, 3]}"))
            val iter = v.iterator()
            assertTrue(iter.hasNext())
            val o1 = iter.next()
            assertNotNull(o1)
            assertFalse(iter.hasNext())
        }
    }
}
