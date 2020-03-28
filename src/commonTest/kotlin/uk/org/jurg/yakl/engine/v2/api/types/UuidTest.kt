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
package uk.org.jurg.yakl.engine.v2.api.types

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuidFrom
import uk.org.jurg.yakl.engine.v2.api.Dump
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.representer.StandardRepresenter
import kotlin.test.Test
import kotlin.test.assertEquals

class UuidTest {

    private val THE_UUID = uuidFrom("37e6a9fa-52d3-11e8-9c2d-fa7ae01bbebc")

    @Test
    fun `Represent UUID as node with global tag`() {
        val standardRepresenter = StandardRepresenter(DumpSettings())
        val node = standardRepresenter.represent(THE_UUID)
        assertEquals("tag:yaml.org,2002:com.benasher44.uuid.Uuid", node.tag.value)
    }

    @Test
    fun `Dump UUID as string`() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val output = dump.dumpToString(THE_UUID)
        assertEquals("!!com.benasher44.uuid.Uuid '37e6a9fa-52d3-11e8-9c2d-fa7ae01bbebc'\n", output)
    }

    @Test
    fun `Parse UUID`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val uuid = load.loadFromString("!!com.benasher44.uuid.Uuid '37e6a9fa-52d3-11e8-9c2d-fa7ae01bbebc'\n") as Uuid
        assertEquals(THE_UUID, uuid)
    }

    @Test
    fun `Parse UUID as root`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val uuid = load.loadFromString("!!com.benasher44.uuid.Uuid '37e6a9fa-52d3-11e8-9c2d-fa7ae01bbebc'\n") as Uuid
        assertEquals(THE_UUID, uuid)
    }
}
