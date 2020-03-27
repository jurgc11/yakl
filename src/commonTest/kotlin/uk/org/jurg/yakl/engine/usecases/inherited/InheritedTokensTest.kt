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
package uk.org.jurg.yakl.engine.usecases.inherited

import uk.org.jurg.yakl.engine.utils.FileInput
import uk.org.jurg.yakl.engine.utils.Files
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader
import uk.org.jurg.yakl.engine.v2.scanner.Scanner
import uk.org.jurg.yakl.engine.v2.scanner.ScannerImpl
import uk.org.jurg.yakl.engine.v2.scanner.StreamReader
import uk.org.jurg.yakl.engine.v2.tokens.StreamEndToken
import uk.org.jurg.yakl.engine.v2.tokens.StreamStartToken
import uk.org.jurg.yakl.engine.v2.tokens.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class InheritedTokensTest : InheritedImportTest() {
    @Test
    fun `Tokens are correct`() {
        val replaces = mapOf(
            Token.ID.Directive to "%",
            Token.ID.DocumentStart to "---",
            Token.ID.DocumentEnd to "...",
            Token.ID.Alias to "*",
            Token.ID.Anchor to "&",
            Token.ID.Tag to "!",
            Token.ID.Scalar to "_",
            Token.ID.BlockSequenceStart to "[[",
            Token.ID.BlockMappingStart to "{{",
            Token.ID.BlockEnd to "]}",
            Token.ID.FlowSequenceStart to "[",
            Token.ID.FlowSequenceEnd to "]",
            Token.ID.FlowMappingStart to "{",
            Token.ID.FlowMappingEnd to "}",
            Token.ID.BlockEntry to ",",
            Token.ID.FlowEntry to ",",
            Token.ID.Key to "?",
            Token.ID.Value to ":"
        )

        val tokensFiles = getStreamsByExtension(".tokens")
        assertTrue(tokensFiles.isNotEmpty(), "No test files found.")
        for (name in tokensFiles) {
            val position = name.lastIndexOf('.')
            val dataName = name.substring(0, position) + ".data"

            val tokenFileData = Files.readFile(name)
            val tokens = tokenFileData.split("\\s+".toRegex())
            // Kotlin regex seems to differ from Java. The final empty part is returned
            // in Kotlin but not Java. Here we drop the last part
            val split = tokens.subList(0, tokens.size-1)

            val tokens2 = ArrayList<String>()
            tokens2.addAll(split)
            //
            val tokens1 = ArrayList<String>()
            val reader = StreamReader(YamlUnicodeReader(FileInput(dataName)), LoadSettings())
            val scanner = ScannerImpl(reader)
            try {
                while (scanner.checkToken()) {
                    val token = scanner.next()
                    if (!(token is StreamStartToken || token is StreamEndToken)) {
                        val replacement = replaces.getValue(token.tokenId)
                        tokens1.add(replacement)
                    }
                }
                assertEquals(tokens1.size, tokens2.size, tokenFileData)
                assertEquals(tokens1, tokens2)
            } catch (e: RuntimeException) {
                println(
                    """
                        File name:
                        $name
                        """.trimIndent()
                )
                val data = Files.readFile(name)
                println("Data: \n$data")
                println("Tokens:")
                for (token in tokens1) {
                    println(token)
                }
                fail("Cannot scan: $name")
            }
        }
    }

    @Test
    fun `Tokens are correct in data files`() {
        val files = getStreamsByExtension(".data", true)
        assertTrue(files.isNotEmpty(), "No test files found.")
        for (file in files) {
            val tokens = ArrayList<String>()
            val input = FileInput(file)
            val reader = StreamReader(YamlUnicodeReader(input), LoadSettings())
            val scanner: Scanner = ScannerImpl(reader)
            try {
                while (scanner.checkToken()) {
                    val token = scanner.next()
                    tokens.add(token::class.simpleName!!)
                }
            } catch (e: RuntimeException) {
                println(
                    """
                    File name:
                    $file
                    """.trimIndent()
                )
                val data = getResource(file)
                println("Data: \n$data")
                println("Tokens:")
                for (token in tokens) {
                    println(token)
                }
                fail("Cannot scan: $file; ${e.message}")
            } finally {
                input.close()
            }
        }
    }
}
