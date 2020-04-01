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

package uk.org.jurg.yakl.engine.utils

import uk.org.jurg.yakl.engine.utils.EnvironmentVariables.clear
import uk.org.jurg.yakl.engine.utils.EnvironmentVariables.get
import uk.org.jurg.yakl.engine.utils.EnvironmentVariables.set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvironmentVariablesTest {
    private val envVarName = "YAKL_UNIT_TEST_VAR_LKSADIOJ"
    private val envVarValue = "testValue"
    private val envVarValue2 = "testValue2"

    @Test
    fun testEnvironmentVariables() {
        clear(envVarName)
        assertNull(get(envVarName))
        set(envVarName, envVarValue)
        assertEquals(get(envVarName), envVarValue)
        set(envVarName, envVarValue2)
        assertEquals(get(envVarName), envVarValue2)
        clear(envVarName)
        assertNull(get(envVarName))
    }
}
