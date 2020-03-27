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


package uk.org.jurg.yakl.engine.v2.composer

import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Compose
import uk.org.jurg.yakl.engine.v2.exceptions.ComposerException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComposerTest {
    @Test
    fun `Fail to Compose one document when more documents are provided.`() {

        val exception = assertFailsWith(ComposerException::class)
        { Compose(LoadSettings()).composeString("a\n---\nb\n") }
        assertTrue(exception.message.contains("expected a single document in the stream"))
        assertTrue(exception.message.contains("but found another document"))
    }

    @Test
    fun failToComposeUnknownAlias() {
        val exception = assertFailsWith(ComposerException::class)
        { Compose(LoadSettings()).composeString("[a, *id b]") }
        val message = exception.message
        assertTrue(message.contains("found undefined alias id"))
    }

    @Test
    fun composeAnchor() {
        val data = "--- &113\n{name: Bill, age: 18}"
        val compose = Compose(LoadSettings())
        val node = compose.composeString(data)
        assertNotNull(node)
        assertEquals("113", node.anchor?.value)
    }
}
