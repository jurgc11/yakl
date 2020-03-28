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
package uk.org.jurg.yakl.engine.usecases.custom_collections


import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UNCHECKED_CAST")
class CustomDefaultCollectionsTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `Create LinkedList by default`() {
        //init size is not used in LinkedList
        val settings = LoadSettings(
            defaultList = { ArrayDeque(it) }
        )
        val load = Load(settings)
        val list = load.loadFromString("- a\n- b") as ArrayDeque<String>
        assertEquals(2, list.size)
    }

    @Test
    fun `Create TreeMap by default`() {
        //init size is not used in TreeMap
        val settings = LoadSettings(
            defaultMap = { mutableMapOf("k0" to "v0") }
        )
        val load = Load(settings)
        val map = load.loadFromString("{k1: v1, k2: v2}") as Map<String, String>
        assertEquals(3, map.size)
    }

    @Test
    fun `Create set with extra element by default`() {
        val settings = LoadSettings(
            defaultSet = { mutableSetOf("Y") }
        )
        val load = Load(settings)

        val set =  load.loadFromString("!!set\n? foo\n? bar") as Set<String>
        assertEquals(3, set.size)
        //must be re-ordered
        assertEquals("Y", set.first())
        assertEquals("bar", set.last())
    }

}
