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
package uk.org.jurg.yakl.engine.v2.constructor

import uk.org.jurg.yakl.engine.v2.api.ConstructNode
import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.Node
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultConstructorTest {
    @Test
    fun constructNullWhenUnknown() {
        val settings = LoadSettings()
        val load = Load(settings, MagicNullConstructor(settings))
        val str = load.loadFromString("!unknownLocalTag a")
        assertNull(str)
    }

    @Test
    fun failWhenUnknown() {
        val load = Load(LoadSettings())

        val ex = assertFails { load.loadFromString("!unknownLocalTag a") }

        assertTrue(ex is YamlEngineException)
        assertTrue(ex.message!!.startsWith("could not determine a constructor for the tag !unknownLocalTag"))
    }
}

/**
 * Make NULL if the tag is not recognised
 */
class MagicNullConstructor(settings: LoadSettings) : StandardConstructor(settings) {

    public override fun findConstructorFor(node: Node): ConstructNode? {
        return ConstructYamlNull()
    }
}
