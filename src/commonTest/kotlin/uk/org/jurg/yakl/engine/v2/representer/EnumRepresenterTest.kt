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

import uk.org.jurg.yakl.engine.v2.api.Dump
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import kotlin.test.Test
import kotlin.test.assertEquals

class EnumRepresenterTest {
    @Test
    fun `Represent Enum as node with global tag`() {
        val settings = DumpSettings(defaultScalarStyle = ScalarStyle.DOUBLE_QUOTED)
        val standardRepresenter = StandardRepresenter(settings)
        val node = standardRepresenter.represent(FormatEnum.JSON) as ScalarNode
        assertEquals(ScalarStyle.DOUBLE_QUOTED, node.style)
        assertEquals("tag:yaml.org,2002:uk.org.jurg.yakl.engine.v2.representer.FormatEnum", node.tag.value)
    }

    @Test
    fun `Dump Enum with ScalarStyle.DOUBLE_QUOTED`() {
        val settings = DumpSettings(defaultScalarStyle = ScalarStyle.DOUBLE_QUOTED)
        val dumper = Dump(settings)
        val node = dumper.dumpToString(FormatEnum.JSON)
        assertEquals("!!uk.org.jurg.yakl.engine.v2.representer.FormatEnum \"JSON\"\n", node)
    }
}
