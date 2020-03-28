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
package uk.org.jurg.yakl.engine.usecases.references

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NonAsciiAnchorTest {
    private val NON_ANCHORS = ":,[]{}"

    @Test
    fun `Non ASCII anchor name must be accepted`() {
        val settings = LoadSettings()
        val load = Load(settings)
        val floatValue = load.loadFromString("&something_タスク タスク") as String?
        assertEquals("タスク", floatValue)
    }

    @Test
    fun `Reject invalid anchors which contain one of $NON_ANCHORS`() {
        for (i in NON_ANCHORS.indices) {
            val ex = assertFailsWith<Exception> { loadWith(NON_ANCHORS[i]) }
            assertTrue(ex.message!!.contains("while scanning an anchor"), ex.message)
            assertTrue(ex.message!!.contains("unexpected character found"), ex.message)
        }
    }

    private fun loadWith(c: Char) {
        val settings = LoadSettings()
        val load = Load(settings)
        load.loadFromString("&$c value")
    }
}
