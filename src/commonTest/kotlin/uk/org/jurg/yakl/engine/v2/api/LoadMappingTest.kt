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

import uk.org.jurg.yakl.engine.v2.utils.TestUtils.getResource
import kotlin.test.Test
import kotlin.test.assertEquals
@Suppress("UNCHECKED_CAST")
internal class LoadMappingTest {
    @Test
    fun `Empty map {} is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val map = load.loadFromString("{}") as Map<Any?, Any?>
        val expected = settings.defaultMap.invoke(0)
        assertEquals(expected, map)
    }

    @Test
    fun `map {a: 1} is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val map = load.loadFromString("{a: 1}") as Map<String, Int>
        val expected = mapOf("a" to 1)
        assertEquals(expected, map)
    }

    @Test
    fun `map {a: 1, b: 2} is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val map = load.loadFromString("a: 1\nb: 2\nc:\n  - aaa\n  - bbb") as Map<String, Any>
        val expected = mapOf("a" to 1, "b" to 2, "c" to listOf("aaa", "bbb"))
        assertEquals(expected, map)
        //assertEquals("{a=1, b=2, c=[aaa, bbb]}", map.toString());
    }

    @Test
    fun `map {x: 1, y: 2, z:3} is parsed`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val map = load.loadFromString(getResource("/load/map1.yaml")) as Map<String, Int>
        val expected = mapOf("x" to 1, "y" to 2, "z" to 3)
        assertEquals(expected, map)
    }
}
