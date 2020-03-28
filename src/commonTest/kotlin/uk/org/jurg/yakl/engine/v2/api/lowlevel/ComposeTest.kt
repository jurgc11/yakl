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
package uk.org.jurg.yakl.engine.v2.api.lowlevel

import kotlinx.io.ByteArrayInput
import uk.org.jurg.yakl.engine.utils.StringReader
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

internal class ComposeTest {
    @Test
    fun composeEmptyReader() {
        val compose = Compose(LoadSettings())
        val node = compose.composeReader(StringReader(""))
        assertNull(node)
    }

    @Test
    fun composeEmptyInputStream() {
        val compose = Compose(LoadSettings())
        val node = compose.composeInputStream(ByteArrayInput(byteArrayOf()))
        assertNull(node)
    }

    @Test
    fun composeAllFromEmptyReader() {
        val compose = Compose(LoadSettings())
        val nodes = compose.composeAllFromReader(StringReader(""))
        assertFalse(nodes.iterator().hasNext())
    }

    @Test
    fun composeAllFromEmptyInputStream() {
        val compose = Compose(LoadSettings())
        val nodes = compose.composeAllFromInputStream(ByteArrayInput(byteArrayOf()))
        assertFalse(nodes.iterator().hasNext())
    }
}
