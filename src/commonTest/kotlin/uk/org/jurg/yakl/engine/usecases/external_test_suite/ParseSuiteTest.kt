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


package uk.org.jurg.yakl.engine.usecases.external_test_suite

import uk.org.jurg.yakl.engine.usecases.external_test_suite.SuiteUtils.getAll
import uk.org.jurg.yakl.engine.usecases.external_test_suite.SuiteUtils.getOne
import uk.org.jurg.yakl.engine.usecases.external_test_suite.SuiteUtils.parseData
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Parse
import uk.org.jurg.yakl.engine.v2.events.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ParseSuiteTest {
    private val all: List<SuiteData> = getAll()
        .filter { !SuiteUtils.deviationsWithSuccess.contains(it.name) }
        .filter { !SuiteUtils.deviationsWithError.contains(it.name) }

    /**
     * This test is used to debug one test (which is given explicitly)
     */
    @Test
    fun `Parse: Run one test`() {
        val data = getOne("6FWR")
        val settings = LoadSettings(
            label = data.label
        )
        Parse(settings).parseString(data.input).forEach { event ->
            assertNotNull(event)
        }
    }

    @Test
    fun `Run comprehensive test suite`() {
        for (data in all) {
            val result = parseData(data)!!
            if (data.error) {
                assertTrue(
                    result.error != null, """
                     Expected error, but got none in file ${data.name}, ${data.label}
                     ${result.events}
                     """.trimIndent()
                )
            } else {
                if (result.error != null) {
                    fail("Expected NO error, but got: " + result.error)
                } else {
                    data.events.zip(result.events) { expected, event ->
                        ParsePair(
                            expected,
                            event
                        )
                    }.forEach {
                        val representation = EventRepresentation(it.event)
                        assertEquals(it.expected, representation.representation, "Failure in " + data.name)
                    }
                }
            }
        }
    }
}

private class ParsePair(val expected: String, val event: Event)
