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
package uk.org.jurg.yakl.engine.v2.api

import uk.org.jurg.yakl.engine.v2.utils.TestUtils.getResource
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UNCHECKED_CAST")
class LoadSequenceTest {

    @Test
    fun `Empty list [] is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val list = load.loadFromString("[]") as List<Int>
        assertEquals(ArrayList(), list)
    }

    @Test
    fun `list [2] is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val list = load.loadFromString("[2]") as List<Int>
        assertEquals(listOf(2), list)
    }

    @Test
    fun `list [2,3] is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val list = load.loadFromString("[2,3]") as List<Int>
        assertEquals(listOf(2, 3), list)
    }

    @Test
    fun `list [2,a,true] is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val list = load.loadFromString("[2,a,true]") as List<Any?>
        assertEquals(listOf(2, "a", true), list)
    }

    @Test
    fun `list is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val list = load.loadFromString(getResource("/load/list1.yaml")) as List<Any>
        assertEquals(listOf("a", "bb", "ccc", "dddd"), list)
    }
}