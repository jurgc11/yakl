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
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader
import uk.org.jurg.yakl.engine.v2.exceptions.ReaderException
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.scanner.StreamReader
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class InheritedReaderTest : InheritedImportTest() {
    @Test
    @Ignore //TODO
    fun `Reader errors`() {
        val inputs = getStreamsByExtension(".stream-error")
        println(inputs)
        inputs.forEach {
            println("Processing $it")
            val input = FileInput(it)
            val unicodeReader = YamlUnicodeReader(input)
            val stream = StreamReader(unicodeReader, LoadSettings())
            try {
                while (stream.peek() != '\u0000'.toInt()) {
                    stream.forward()
                }
                fail("Invalid stream must not be accepted: $it; encoding=${unicodeReader.encoding}")
            } catch (e: ReaderException) {
                assertTrue(e.toString().contains(" special characters are not allowed"), e.toString())
            } catch (e: YamlEngineException) {
                assertTrue(e.toString().contains("MalformedInputException"), e.toString())
            } finally {
                input.close()
            }
        }
    }
}
