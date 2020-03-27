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
package uk.org.jurg.yakl.engine.core_schema

import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberTest {
    @Test
    fun `Test all integers which are define in the core schema & JSON`() {
        val loader = Load(LoadSettings())
        assertEquals(1, loader.loadFromString("1"))
        assertEquals(-1, loader.loadFromString("-1"))
        assertEquals(0, loader.loadFromString("0"))
        assertEquals(0, loader.loadFromString("-0"))
        assertEquals("012", loader.loadFromString("012"), "Leading zeros are not allowed.")
        assertEquals(1234567890, loader.loadFromString("1234567890"))
        assertEquals(12345678901L, loader.loadFromString("12345678901"))
        //assertEquals(BigInteger("1234567890123456789123"), loader.loadFromString("1234567890123456789123"))
    }

    @Test
    fun `Test all strings which WERE integers or doubles in YAML 1.1`() {
        val loader = Load(LoadSettings())
        assertEquals("12:10:02", loader.loadFromString("12:10:02"))
        assertEquals("0b1010", loader.loadFromString("0b1010"))
        assertEquals("0xFF", loader.loadFromString("0xFF"))
        assertEquals("1_000", loader.loadFromString("1_000"))
        assertEquals("1_000.5", loader.loadFromString("1_000.5"))
        assertEquals(".inf", loader.loadFromString(".inf"))
        assertEquals("+.inf", loader.loadFromString("+.inf"))
        assertEquals("-.inf", loader.loadFromString("-.inf"))
        assertEquals(".nan", loader.loadFromString(".nan"))

        //start with +
        assertEquals("+1", loader.loadFromString("+1"))
        assertEquals("+1223344", loader.loadFromString("+1223344"))
        assertEquals("+12.23344", loader.loadFromString("+12.23344"))
        assertEquals("+0.23344", loader.loadFromString("+0.23344"))
        assertEquals("+0", loader.loadFromString("+0"))

        //leading zero
        assertEquals("03", loader.loadFromString("03"))
        assertEquals("03.67", loader.loadFromString("03.67"))
    }

    @Test
    fun `Test all doubles which are define in the core schema & JSON`() {
        val loader = Load(LoadSettings())
        assertEquals(-1.345, loader.loadFromString("-1.345"))
        assertEquals(-0.0, loader.loadFromString("-0.0"))
        assertEquals(0.123, loader.loadFromString("0.123"))
        assertEquals(1.23E-6, loader.loadFromString("1.23e-6"))
        assertEquals(1.23E6, loader.loadFromString("1.23e+6"))
        assertEquals(1.23E6, loader.loadFromString("1.23E6"))
        assertEquals(-1.23E6, loader.loadFromString("-1.23E6"))
        assertEquals(1000.25, loader.loadFromString("1000.25"))
        assertEquals(9000.0, loader.loadFromString("9000.00"))
    }
}
