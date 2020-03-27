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

import uk.org.jurg.yakl.engine.v2.resolver.ENV_FORMAT
import uk.org.jurg.yakl.engine.v2.resolver.NAME_GROUP
import uk.org.jurg.yakl.engine.v2.resolver.SEPARATOR_GROUP
import uk.org.jurg.yakl.engine.v2.resolver.VALUE_GROUP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
${VARIABLE:-default} evaluates to default if VARIABLE is unset or empty in the environment.
${VARIABLE-default} evaluates to default only if VARIABLE is unset in the environment.

Similarly, the following syntax allows you to specify mandatory variables:

${VARIABLE:?err} exits with an error message containing err if VARIABLE is unset or empty in the environment.
${VARIABLE?err} exits with an error message containing err if VARIABLE is unset in the environment.
 */
class EnvFormatTest {
    @Test
    fun testMatchBasic() {
        assertTrue(ENV_FORMAT.matches("\${V}"))
        assertTrue(ENV_FORMAT.matches("\${PATH}"))
        assertTrue(ENV_FORMAT.matches("\${VARIABLE}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE }"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE}"))
        assertTrue(ENV_FORMAT.matches("\${\tVARIABLE  }"))
        val groupValues = ENV_FORMAT.find("\${VARIABLE}")!!.groups
        assertEquals("VARIABLE", groupValues[NAME_GROUP]?.value)
        assertNull(groupValues[VALUE_GROUP])
        assertNull(groupValues[SEPARATOR_GROUP])
        assertFalse(ENV_FORMAT.matches("\${VARI ABLE}"))
    }

    @Test
    fun testMatchDefault() {
        assertTrue(ENV_FORMAT.matches("\${VARIABLE-default}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE-default}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE-default }"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE-default}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE-}"))
        val groupValues = ENV_FORMAT.find("\${VARIABLE-default}")!!.groupValues
        assertEquals("VARIABLE", groupValues[NAME_GROUP])
        assertEquals("default", groupValues[VALUE_GROUP])
        assertEquals("-", groupValues[SEPARATOR_GROUP])
        assertFalse(ENV_FORMAT.matches("\${VARIABLE -default}"))
        assertFalse(ENV_FORMAT.matches("\${VARIABLE - default}"))
        assertFalse(ENV_FORMAT.matches("\${VARIABLE -default}"))
    }

    @Test
    fun testMatchDefaultOrEmpty() {
        assertTrue(ENV_FORMAT.matches("\${VARIABLE:-default}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE:-default }"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE:-}"))
        val groupValues = ENV_FORMAT.find("\${VARIABLE:-default}")!!.groupValues

        assertEquals("VARIABLE", groupValues[NAME_GROUP])
        assertEquals("default", groupValues[VALUE_GROUP])
        assertEquals(":-", groupValues[SEPARATOR_GROUP])
        assertFalse(ENV_FORMAT.matches("\${VARIABLE :-default}"))
        assertFalse(ENV_FORMAT.matches("\${VARIABLE : -default}"))
        assertFalse(ENV_FORMAT.matches("\${VARIABLE : - default}"))
    }

    @Test
    fun testMatchErrorDefaultOrEmpty() {
        assertTrue(ENV_FORMAT.matches("\${VARIABLE:?err}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE:?err }"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE:? }"))
        val groupValues = ENV_FORMAT.find("\${VARIABLE:?err}")!!.groupValues
        assertEquals("VARIABLE", groupValues[NAME_GROUP])
        assertEquals("err", groupValues[VALUE_GROUP])
        assertEquals(":?", groupValues[SEPARATOR_GROUP])
        assertFalse(ENV_FORMAT.matches("\${ VARIABLE :?err }"))
        assertFalse(ENV_FORMAT.matches("\${ VARIABLE : ?err }"))
        assertFalse(ENV_FORMAT.matches("\${ VARIABLE : ? err }"))
    }

    @Test
    fun testMatchErrorDefault() {
        assertTrue(ENV_FORMAT.matches("\${VARIABLE?err}"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE:?err }"))
        assertTrue(ENV_FORMAT.matches("\${ VARIABLE:?}"))
        val groupValues = ENV_FORMAT.find("\${ VARIABLE?err }")!!.groupValues
        assertEquals("VARIABLE", groupValues[NAME_GROUP])
        assertEquals("err", groupValues[VALUE_GROUP])
        assertEquals("?", groupValues[SEPARATOR_GROUP])
        assertFalse(ENV_FORMAT.matches("\${ VARIABLE ?err }"))
        assertFalse(ENV_FORMAT.matches("\${ VARIABLE ?err }"))
        assertFalse(ENV_FORMAT.matches("\${ VARIABLE ? err }"))
    }
}
