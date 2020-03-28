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
package uk.org.jurg.yakl.engine.v2.issues.issue1

import uk.org.jurg.yakl.engine.v2.api.ConstructNode
import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NullConstructorTest {
    @Test
    fun customConstructorMustBeCalledWithoutNode() {
        val tagConstructors = mapOf(Tag.NULL to MyConstructNull())

        val settings = LoadSettings(
            tagConstructors = tagConstructors
        )
        val loader = Load(settings)
        assertNotNull(loader.loadFromString(""), "Expected MyConstructNull to be called.")
        assertEquals("absent", loader.loadFromString(""), "Expected MyConstructNull to be called.")
    }

    @Test
    fun customConstructorMustBeCalledWithNode() {
        val tagConstructors = mapOf(Tag.NULL to MyConstructNull())

        val settings: LoadSettings = LoadSettings(
            tagConstructors = tagConstructors
        )
        val loader = Load(settings)
        assertEquals("present", loader.loadFromString("!!null null"), "Expected MyConstructNull to be called.")
    }

    private inner class MyConstructNull : ConstructNode {
        override fun construct(node: Node?): Any? {
            return if (node == null) "absent" else "present"
        }
    }
}
