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


package uk.org.jurg.yakl.engine.usecases.inherited

import uk.org.jurg.yakl.engine.utils.Files
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.tokens.Token
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InheritedCanonicalTest : InheritedImportTest() {
    @Test
    fun `Canonical scan`() {
        val files = getStreamsByExtension(".canonical")
        assertTrue(files.isNotEmpty(), "No test files found.")
        files.forEach {
            val input = Files.readFile(it)
            val tokens = canonicalScan(input, it)
            assertFalse(tokens.isEmpty())
        }
    }

    private fun canonicalScan(input: String, label: String): List<Token> {

        val scanner = CanonicalScanner(input, label)
        return scanner.asSequence().filterNotNull().toList()
    }

    @Test
    fun `Canonical parse`() {
        val files = getStreamsByExtension(".canonical")
        assertTrue(files.isNotEmpty(), "No test files found.")
        files.forEach {
            val input = Files.readFile(it)
            val tokens: List<Event> = canonicalParse(input, it)
            assertFalse(tokens.isEmpty())
        }
    }
}
