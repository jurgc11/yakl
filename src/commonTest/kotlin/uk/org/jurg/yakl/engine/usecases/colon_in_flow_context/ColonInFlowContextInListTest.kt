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


package uk.org.jurg.yakl.engine.usecases.colon_in_flow_context

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("UNCHECKED_CAST")
internal class ColonInFlowContextInListTest {
    @Test
    fun withSpacesAround() {
        val loader = Load(LoadSettings())
        val list = loader.loadFromString("[ http://foo ]") as List<String>
        assertTrue(list.contains("http://foo"))
    }

    @Test
    fun withoutSpacesAround() {
        val loader = Load(LoadSettings())
        val list = loader.loadFromString("[http://foo]") as List<String>
        assertTrue(list.contains("http://foo"))
    }

    @Test
    fun twoValues() {
        val loader = Load(LoadSettings())
        val list = loader.loadFromString("[ http://foo,http://bar ]") as List<String>
        assertTrue(list.contains("http://foo"))
        assertTrue(list.contains("http://bar"))
    }
}
