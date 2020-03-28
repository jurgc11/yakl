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
package uk.org.jurg.yakl.engine.usecases.recursive

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.utils.TestUtils.getResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecursiveSetTest {
    @Test
    fun `Fail to load map with recursive keys`() {
        val recursiveInput = getResource("/recursive/recursive-set-1.yaml")
        val load = Load( LoadSettings())
        //fail to load map which has only one key - reference to itself
        assertFailsWith(YamlEngineException::class, "Recursive key for mapping is detected but it is not configured to be allowed.")
        { load.loadFromString(recursiveInput) }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Load map with recursive keys if it is explicitly allowed`() {
        val recursiveInput = getResource("/recursive/recursive-set-1.yaml")
        val settings = LoadSettings(
            allowRecursiveKeys = true
        )
        val load = Load(settings)
        //load map which has only one key - reference to itself
        val recursive = load.loadFromString(recursiveInput) as Set<Any>
        assertEquals(3, recursive.size)
    }
}
