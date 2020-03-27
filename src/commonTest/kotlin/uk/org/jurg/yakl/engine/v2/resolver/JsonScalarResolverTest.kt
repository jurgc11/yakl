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
package uk.org.jurg.yakl.engine.v2.resolver

import uk.org.jurg.yakl.engine.v2.nodes.Tag
import kotlin.test.Test
import kotlin.test.assertEquals

//@org.junit.jupiter.api.Tag("fast")
class JsonScalarResolverTest {
    private val scalarResolver = JsonScalarResolver()

    @Test
    fun `Resolve explicit scalar`() {
        assertEquals(Tag.STR, scalarResolver.resolve("1", false))
    }

    @Test
    fun `Resolve implicit integer`() {
        assertEquals(Tag.INT, scalarResolver.resolve("1", true))
        assertEquals(Tag.INT, scalarResolver.resolve("112233", true))
        assertEquals(Tag.INT, scalarResolver.resolve("-1", true))
        assertEquals(Tag.STR, scalarResolver.resolve("+1", true))
        assertEquals(Tag.STR, scalarResolver.resolve("-01", true))
        assertEquals(Tag.STR, scalarResolver.resolve("013", true))
        assertEquals(Tag.INT, scalarResolver.resolve("0", true))
    }

    @Test
    fun `Resolve implicit float`() {
        assertEquals(Tag.FLOAT, scalarResolver.resolve("1.0", true))
        assertEquals(Tag.FLOAT, scalarResolver.resolve("-1.3", true))
        assertEquals(Tag.STR, scalarResolver.resolve("+01.445", true))
        assertEquals(Tag.FLOAT, scalarResolver.resolve("-1.455e45", true))
        assertEquals(Tag.FLOAT, scalarResolver.resolve("1.455E-045", true))
        assertEquals(Tag.FLOAT, scalarResolver.resolve("0.0", true))
        assertEquals(Tag.STR, scalarResolver.resolve("+1", true))
    }

    @Test
    fun `Resolve implicit boolean`() {
        assertEquals(Tag.BOOL, scalarResolver.resolve("true", true))
        assertEquals(Tag.BOOL, scalarResolver.resolve("false", true))
        assertEquals(Tag.STR, scalarResolver.resolve("False", true))
        assertEquals(Tag.STR, scalarResolver.resolve("FALSE", true))
        assertEquals(Tag.STR, scalarResolver.resolve("off", true))
        assertEquals(Tag.STR, scalarResolver.resolve("no", true))
    }

    @Test
    fun `Resolve implicit null`() {
        assertEquals(Tag.NULL, scalarResolver.resolve("null", true))
        assertEquals(Tag.NULL, scalarResolver.resolve("", true))
    }

    @Test
    fun `Resolve implicit strings`() {
        assertEquals(Tag.STR, scalarResolver.resolve(".inf", true))
        assertEquals(Tag.STR, scalarResolver.resolve("0xFF", true))
        assertEquals(Tag.STR, scalarResolver.resolve("True", true))
        assertEquals(Tag.STR, scalarResolver.resolve("TRUE", true))
        assertEquals(Tag.STR, scalarResolver.resolve("NULL", true))
        assertEquals(Tag.STR, scalarResolver.resolve("~", true))
    }
}
