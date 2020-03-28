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
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * https://en.wikipedia.org/wiki/Billion_laughs_attack#Variations
 */
class BillionLaughsAttackTest {
    @Test
    fun `Load many aliases if explicitly allowed`() {
        val settings = LoadSettings(
            maxAliasesForCollections = 72
        )
        val load = Load(settings)
        val map = load.loadFromString(data)
        assertNotNull(map)
    }

    @Test
    @Ignore //TODO
    fun `Billion_laughs_attack if data expanded`() {
        val settings = LoadSettings(
            maxAliasesForCollections = 100
        )
        val load = Load(settings)
        val map = load.loadFromString(data)
        assertNotNull(map)
        val ex = assertFailsWith<Throwable> { map.toString() }
        assertTrue(ex.message!!.contains("heap"))
    }

    @Test
    fun `Prevent Billion_laughs_attack by default`() {
        val settings = LoadSettings()
        val load = Load(settings)
        assertFailsWith<YamlEngineException>("Number of aliases for non-scalar nodes exceeds the specified max=50")
        { load.loadFromString(data) }
    }

    @Test
    fun `Number of aliases for scalar nodes is not restricted`() {
        val settings = LoadSettings(
            maxAliasesForCollections = 5
        )
        val load = Load(settings)
        load.loadFromString(scalarAliasesData)
    }

    companion object {
        const val data = "a: &a [\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\",\"lol\"]\n" +
                "b: &b [*a,*a,*a,*a,*a,*a,*a,*a,*a]\n" +
                "c: &c [*b,*b,*b,*b,*b,*b,*b,*b,*b]\n" +
                "d: &d [*c,*c,*c,*c,*c,*c,*c,*c,*c]\n" +
                "e: &e [*d,*d,*d,*d,*d,*d,*d,*d,*d]\n" +
                "f: &f [*e,*e,*e,*e,*e,*e,*e,*e,*e]\n" +
                "g: &g [*f,*f,*f,*f,*f,*f,*f,*f,*f]\n" +
                "h: &h [*g,*g,*g,*g,*g,*g,*g,*g,*g]\n" +
                "i: &i [*h,*h,*h,*h,*h,*h,*h,*h,*h]"
        const val scalarAliasesData = "a: &a foo\n" +
                "b:  *a\n" +
                "c:  *a\n" +
                "d:  *a\n" +
                "e:  *a\n" +
                "f:  *a\n" +
                "g:  *a\n"
    }
}
