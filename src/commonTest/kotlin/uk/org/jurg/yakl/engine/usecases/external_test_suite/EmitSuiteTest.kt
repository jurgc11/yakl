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
package uk.org.jurg.yakl.engine.usecases.external_test_suite

import uk.org.jurg.yakl.engine.usecases.external_test_suite.SuiteUtils.getAll
import uk.org.jurg.yakl.engine.usecases.external_test_suite.SuiteUtils.parseData
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Compose
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Present
import kotlin.test.Test
import kotlin.test.assertTrue

class EmitSuiteTest {
    private val all: List<SuiteData> = getAll()
        .filter { !SuiteUtils.deviationsWithSuccess.contains(it.name) }
        .filter { !SuiteUtils.deviationsWithError.contains(it.name) }

    @Test
    fun `Emit test suite`() {
        for (data in all) {
            val result = parseData(data)
            if (data.error) {
                assertTrue(
                    result!!.error != null, """
                     Expected error, but got none in file ${data.name}, ${data.label}
                     ${result.events}
                     """.trimIndent()
                )
            } else {
                val emit = Present(DumpSettings())
                //emit without errors
                val yaml = emit.emitToString(result!!.events.iterator())
                //eat your own dog food
                Compose(LoadSettings()).composeAllFromString(yaml)
            }
        }
    }
}
