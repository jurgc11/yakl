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


package uk.org.jurg.yakl.engine.v2.scanner

import uk.org.jurg.yakl.engine.utils.StringReader
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.tokens.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScannerTest {
    @Test
    fun `Expected NoSuchElementException after all the tokens are finished.`() {
        val reader = StreamReader(StringReader("444222"), LoadSettings())
        val scanner = ScannerImpl(reader)
        assertTrue(scanner.hasNext())
        assertEquals(Token.ID.StreamStart, scanner.next().tokenId)
        assertTrue(scanner.hasNext())
        assertEquals(Token.ID.Scalar, scanner.next().tokenId)
        assertTrue(scanner.hasNext())
        assertEquals(Token.ID.StreamEnd, scanner.next().tokenId)
        assertFalse(scanner.hasNext())
        assertFailsWith(NoSuchElementException::class, "No more Tokens found.") {  scanner.next()}
    }
}
