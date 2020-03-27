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

import uk.org.jurg.yakl.engine.v2.exceptions.DuplicateKeyException
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.resolver.JsonScalarResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class LoadSettingsTest {
    @Test
    fun `Accept only YAML 1.2`() {
        val settings = LoadSettings(
            versionFunction = { if (it.major == 1 && it.minor == 2) it else throw IllegalArgumentException("Only 1.2 is supported."); }
        )

        val load = Load(settings)
        assertFailsWith(
            IllegalArgumentException::class,
            "Only 1.2 is supported."
        ) { load.loadFromString("%YAML 1.1\n...\nfoo") }
    }

    @Test
    fun `Do not allow duplicate keys`() {
        val settings = LoadSettings(allowDuplicateKeys = false)
        val load = Load(settings)
        val ex = assertFailsWith<DuplicateKeyException> { load.loadFromString("{a: 1, a: 2}") }
        assertTrue(ex.message.contains("found duplicate key a"))
    }

    @Test
    fun `Do not allow duplicate keys by default`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val ex = assertFailsWith<DuplicateKeyException> { load.loadFromString("{a: 1, a: 2}") }
        assertTrue(ex.message.contains("found duplicate key a"))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Allow duplicate keys`() {
        val settings = LoadSettings(
            allowDuplicateKeys = true
        )
        val load = Load(settings)
        val map = load.loadFromString("{a: 1, a: 2}") as Map<String, Int>
        assertEquals(2, map["a"])
    }

    @Test
    fun `Set and get custom property`() {
        val key = SomeKey()
        val settings = LoadSettings(
            customProperties = mapOf(key to "foo", SomeStatus.DELIVERED to "bar")
        )

        assertEquals("foo", settings.customProperties[key])
        assertEquals("bar", settings.customProperties[SomeStatus.DELIVERED])
    }

    class SomeKey : SettingKey
    enum class SomeStatus : SettingKey {
        ORDERED, DELIVERED
    }

    @Test
    fun `Use custom ScalarResolver`() {
        val settings = LoadSettings(
            scalarResolver = SomeScalarResolver()
        )

        val load = Load(settings)
        assertEquals("false", load.loadFromString("false"))
        assertEquals(1024, settings.bufferSize)
    }

    class SomeScalarResolver : JsonScalarResolver() {

        override fun resolve(value: String, implicit: Boolean): Tag {
            return if ("false" == value) {
                Tag.STR
            } else {
                super.resolve(value, implicit)
            }
        }
    }
}
