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
package uk.org.jurg.yakl.engine.usecases.env

import uk.org.jurg.yakl.engine.utils.EnvironmentVariables
import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.env.EnvConfig
import uk.org.jurg.yakl.engine.v2.exceptions.MissingEnvironmentVariableException
import uk.org.jurg.yakl.engine.v2.utils.TestUtils.getResource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EnvVariableTest {
    @BeforeTest
    fun setup() {
        EnvironmentVariables.set(KEY1, VALUE1)
        EnvironmentVariables.set(EMPTY, "")
    }

    @AfterTest
    fun tearDown() {
        EnvironmentVariables.clear(KEY1)
        EnvironmentVariables.clear(EMPTY)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Parse docker-compose.yaml example`() {
        val loader = Load(LoadSettings(envConfig = object : EnvConfig {}))
        val resource = getResource("/env/docker-compose.yaml")
        val compose = loader.loadFromString(resource) as Map<String, Any>?
        val output = compose.toString()
        assertTrue(
            output.endsWith("environment={URL1=EnvironmentValue1, URL2=, URL3=server3, URL4=, URL5=server5, URL6=server6}}}}"),
            output
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Custom ENV config example`() {
        val provided: HashMap<String, String> = HashMap()
        provided[KEY1] = "VVVAAA111"
        EnvironmentVariables.set(EMPTY, "VVVAAA222")
        val loader = Load(
            LoadSettings(
                envConfig = CustomEnvConfig(provided)
            )
        )
        val resource = getResource("/env/docker-compose.yaml")
        val compose = loader.loadFromString(resource) as Map<String, Any>?
        val output = compose.toString()
        assertTrue(
            output.endsWith("environment={URL1=VVVAAA111, URL2=VVVAAA222, URL3=VVVAAA222, URL4=VVVAAA222, URL5=server5, URL6=server6}}}}"),
            output
        )
    }

    private fun load(template: String): String? {
        val loader = Load(LoadSettings(envConfig = object : EnvConfig {}))
        return loader.loadFromString(template) as String?
    }

    @Test
    fun testEnvironmentSet() {
        assertEquals(
            VALUE1,
            EnvironmentVariables.get(KEY1),
            "Surefire plugin must set the variable."
        )
        assertEquals(
            "",
            EnvironmentVariables.get(EMPTY),
            "Surefire plugin must set the variable."
        )
    }

    @Test
    fun `Parsing ENV variables must be explicitly enabled`() {
        val loader = Load(LoadSettings())
        val loaded = loader.loadFromString("\${EnvironmentKey1}") as String?
        assertEquals("\${EnvironmentKey1}", loaded)
    }

    @Test
    fun `Parsing ENV variable which is defined and not empty`() {
        assertEquals(VALUE1, load("\${EnvironmentKey1}"))
        assertEquals(VALUE1, load("\${EnvironmentKey1-any}"))
        assertEquals(VALUE1, load("\${EnvironmentKey1:-any}"))
        assertEquals(VALUE1, load("\${EnvironmentKey1:?any}"))
        assertEquals(VALUE1, load("\${EnvironmentKey1?any}"))
    }

    @Test
    fun `Parsing ENV variable which is defined as empty`() {
        assertEquals("", load("\${EnvironmentEmpty}"))
        assertEquals("", load("\${EnvironmentEmpty?}"))
        assertEquals("detected", load("\${EnvironmentEmpty:-detected}"))
        assertEquals("", load("\${EnvironmentEmpty-detected}"))
        assertEquals("", load("\${EnvironmentEmpty?detectedError}"))
        val ex = assertFailsWith<MissingEnvironmentVariableException> { load("\${EnvironmentEmpty:?detectedError}") }
        assertEquals("Empty mandatory variable EnvironmentEmpty: detectedError", ex.message)
    }

    @Test
    fun `Parsing ENV variable which is not set`() {
        assertEquals("", load("\${EnvironmentUnset}"))
        assertEquals("", load("\${EnvironmentUnset:- }"))
        assertEquals("detected", load("\${EnvironmentUnset:-detected}"))
        assertEquals("detected", load("\${EnvironmentUnset-detected}"))
        assertFailsWith<MissingEnvironmentVariableException>("Missing mandatory variable EnvironmentUnset: detectedError")
        { load("\${EnvironmentUnset:?detectedError}") }
        assertFailsWith<MissingEnvironmentVariableException>("Missing mandatory variable EnvironmentUnset: detectedError")
        { load("\${EnvironmentUnset?detectedError}") }
    }

    companion object {
        // the variables EnvironmentKey1 and EnvironmentEmpty are set by Maven
        private const val KEY1 = "EnvironmentKey1"
        private const val EMPTY = "EnvironmentEmpty"
        private const val VALUE1 = "EnvironmentValue1"
    }
}
