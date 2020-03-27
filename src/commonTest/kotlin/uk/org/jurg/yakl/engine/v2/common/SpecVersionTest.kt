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
package uk.org.jurg.yakl.engine.v2.common

import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Compose
import uk.org.jurg.yakl.engine.v2.exceptions.YamlVersionException
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpecVersionTest {
    @Test
    fun `Version 1.2 is accepted`() {
        val settings = LoadSettings(
            label = "spec 1.2"
        )
        val node = Compose(settings).composeString("%YAML 1.2\n---\nfoo") as ScalarNode
        assertEquals("foo", node.value)
    }

    @Test
    fun `Version 1.3 is accepted by default`() {
        val settings = LoadSettings(
            label = "spec 1.3"
        )
        val node = Compose(settings).composeString("%YAML 1.3\n---\nfoo") as ScalarNode
        assertEquals("foo", node.value)
    }

    @Test
    fun `Version 1.3 is rejected if configured`() {
        val settings: LoadSettings = LoadSettings(
            label = "spec 1.3",
            versionFunction = { version ->
                require(version.minor <= 2) { "Too high." }
                version
            }
        )

        val exception  = assertFailsWith<IllegalArgumentException>
        { Compose(settings).composeString("%YAML 1.3\n---\nfoo" ) }
        assertEquals("Too high.", exception.message)
    }

    @Test
    fun `Version 2.0 is rejected`() {
        val settings = LoadSettings(
            label = "spec 2.0"
        )
        val exception = assertFailsWith<YamlVersionException>
        { Compose(settings).composeString("%YAML 2.0\n---\nfoo") }
        assertEquals("SpecVersion(major=2, minor=0)", exception.message)
    }
}
