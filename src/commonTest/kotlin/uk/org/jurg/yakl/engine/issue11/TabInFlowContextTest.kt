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
package uk.org.jurg.yakl.engine.issue11

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class TabInFlowContextTest {
    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Do not fail to parse if TAB is used (issue 11)`() {
        val loader = Load(LoadSettings())
        val list = loader.loadFromString("{\n\t\"x\": \"y\"\n}") as Map<String, String>
        assertEquals(1, list.size)
        assertEquals("y", list["x"])
    }
}