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
package uk.org.jurg.yakl.engine.usecases.colon_in_flow_context

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("UNCHECKED_CAST")
class ColonInFlowContextInMapTest {
    @Test
    fun withSeparation() {
        val loader = Load(LoadSettings())
        val map = loader.loadFromString("{a: 1}") as Map<String, Int>
        assertEquals(1, map["a"])
    }

    @Test
    fun withoutEmptyValue() {
        val loader = Load(LoadSettings())
        val map = loader.loadFromString("{a:}") as Map<String, Int>
        assertTrue(map.containsKey("a"))
    }

    @Test
    fun withoutSeparation() {
        val loader = Load(LoadSettings())
        val map = loader.loadFromString("{a:1}") as Map<String, Int>
        assertTrue(map.containsKey("a:1"))
    }
}
