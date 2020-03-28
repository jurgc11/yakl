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
package uk.org.jurg.yakl.engine.v2.representer

import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StandardRepresenterTest {
    private val standardRepresenter = StandardRepresenter(DumpSettings())

    @Test
    fun `Represent unknown class`() {
        assertFailsWith(YamlEngineException::class, "Representer is not defined for class com.google.common.collect.TreeRangeSet")
        { standardRepresenter.represent(Pair("", 1)) }
    }

    @Test
    fun `Represent Enum as node with global tag`() {
        val node = standardRepresenter.represent(FormatEnum.JSON)
        assertEquals("tag:yaml.org,2002:uk.org.jurg.yakl.engine.v2.representer.FormatEnum", node.tag.value)
    }
}
