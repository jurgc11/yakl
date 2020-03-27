/*
 * Copyright 2020 Chris Clifton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package uk.org.jurg.yakl.engine.usecases.tags

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Example of parsing a local tag
 */
class SetsTagTest {
    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Test that !!set tag creates a Set`() {
        val settings = LoadSettings()
        val loader = Load(settings)
        val yaml = """---
sets: !!set
    ? a
    ? b
"""
        val map = loader.loadFromString(yaml) as Map<String, Set<String>>
        val set = map["sets"]!!
        assertEquals(2, set.size)
        val iter = set.iterator()
        assertEquals("a", iter.next())
        assertEquals("b", iter.next())
    }
}
