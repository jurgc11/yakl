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
package uk.org.jurg.yakl.engine.v2.issues.issue3

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ExceptionTest {
    @Test
    fun sequenceException() {
        val load = Load(LoadSettings())
        val exception = assertFailsWith<YamlEngineException> { load.loadFromString("!!seq abc") }
        val message = assertNotNull(exception.message)
        assertTrue(message.contains("kotlin.ClassCastException"))
        assertTrue(message.contains("uk.org.jurg.yakl.engine.v2.nodes.ScalarNode"))
        assertTrue(message.contains("cannot be cast to"))
        assertTrue(message.contains("uk.org.jurg.yakl.engine.v2.nodes.SequenceNode"))
    }

    @Test
    fun intException() {
        val load = Load(LoadSettings())
        val exception = assertFailsWith<YamlEngineException> { load.loadFromString("!!int abc") }
        assertEquals("kotlin.NumberFormatException: For input string: \"abc\"", exception.message)
    }
}
