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
package uk.org.jurg.yakl.engine.usecases.tags

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Example of parsing a local tag
 */
class LocalTagTest {
    @Test
    fun testLocalTag() {
        val tagConstructors = mapOf(Tag("!ImportValue") to CustomConstructor())
        //register to call CustomConstructor when the Tag !ImportValue is found

        val settings = LoadSettings(
            tagConstructors = tagConstructors
        )

        val loader = Load(settings)
        val obj = loader.loadFromString("VpcId: !ImportValue SpokeVPC") as Map<String, ImportValueImpl>
        assertEquals("SpokeVPC", obj["VpcId"]?.value)
    }
}
