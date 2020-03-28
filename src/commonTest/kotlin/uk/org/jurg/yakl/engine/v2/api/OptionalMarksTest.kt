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
package uk.org.jurg.yakl.engine.v2.api

import uk.org.jurg.yakl.engine.usecases.external_test_suite.SuiteUtils
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Compose
import uk.org.jurg.yakl.engine.v2.exceptions.ParserException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class OptionalMarksTest {
    @Test
    fun `Compose: no marks`() {
        val data = SuiteUtils.getOne("2AUY")
        val settings = LoadSettings(
            label = data.label,
            useMarks = false
        )
        val node = Compose(settings).composeString("{a: 4}")
        assertNotNull(node)
    }

    @Test
    fun `Compose: failure with marks`() {
        val data = SuiteUtils.getOne("2AUY")
        val settings = LoadSettings(
            label = data.label,
            useMarks = true
        )
        val exception = assertFailsWith<ParserException>
        { Compose(settings).composeString("{a: 4}}") }
        assertTrue(exception.message.contains("line 1, column 7:"), "The error must contain Mark data.")
    }

    @Test
    fun `Compose: failure without marks`() {
        val data = SuiteUtils.getOne("2AUY")
        val settings = LoadSettings(
            label = data.label,
            useMarks = false
        )
        val exception = assertFailsWith<ParserException>
        { Compose(settings).composeString("{a: 4}}") }
        assertEquals("expected '<document start>', but found '}'\n", exception.message)
    }
}
