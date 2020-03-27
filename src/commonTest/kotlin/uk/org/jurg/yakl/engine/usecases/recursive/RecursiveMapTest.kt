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


package uk.org.jurg.yakl.engine.usecases.recursive


import uk.org.jurg.yakl.engine.v2.api.Dump
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("UNCHECKED_CAST")
class RecursiveMapTest {
    @Test
    fun `Load map with recursive values`() {
        val load = Load(LoadSettings())
        val map = load.loadFromString(
            """
                First occurrence: &anchor Foo
                Second occurrence: *anchor
                Override anchor: &anchor Bar
                Reuse anchor: *anchor

                """.trimIndent()
        ) as Map<String, String>
        val expected: Map<String, String> = mapOf(
            "First occurrence" to "Foo",
            "Second occurrence" to "Foo",
            "Override anchor" to "Bar",
            "Reuse anchor" to "Bar"
        )
        assertEquals(expected, map)
    }

    @Test
    @Ignore //TODO this fails because Kotlin hashCode gets into an infinite loop
    fun `Dump and Load map with recursive values`() {
        val map1 = mutableMapOf<String, Any>("name" to "first")
        val map2 = mutableMapOf<String, Any>("name" to "second")
        map1["next"] = map2
        map2["next"] = map1
        val dump = Dump(DumpSettings())
        val output1 = dump.dumpToString(map1)
        assertEquals(
            """&id002
                next:
                  next: *id002
                  name: second
                name: first
                """.trimIndent(), output1
        )
        val load = Load(LoadSettings())
        val parsed1 = load.loadFromString(output1) as Map<String, Any>
        assertEquals(2, parsed1.size)
        assertEquals("first", parsed1["name"])
        val next = parsed1["next"] as Map<String, Any>
        assertEquals("second", next["name"])
    }

    @Test
    fun `Fail to load map with recursive keys`() {
        val load = Load(LoadSettings())
        //fail to load map which has only one key - reference to itself
        assertFailsWith(YamlEngineException::class, "Recursive key for mapping is detected but it is not configured to be allowed.")
        { load.loadFromString(
            """
                &id002
                *id002: foo
                """.trimIndent()
        ) }
    }

    @Test
    fun `Load map with recursive keys if it is explicitly allowed`() {
        val settings = LoadSettings(
            allowRecursiveKeys = true
        )
        val load = Load(settings)
        //load map which has only one key - reference to itself
        val recursive = load.loadFromString(
            """
                &id002
                *id002: foo
                """.trimIndent()
        ) as Map<Any, Any>
        assertEquals(1, recursive.size)
    }

    @Test
    @Ignore //TODO this fails because Kotlin hashCode gets into an infinite loop
    fun recursiveSet() {
        val map1 = mutableMapOf<String, Any>("name" to "first")
        val map2 = mutableMapOf<String, Any>("name" to "second")
        map1["next"] = map2
        map2["next"] = map1
        map1.hashCode()
    }
}
