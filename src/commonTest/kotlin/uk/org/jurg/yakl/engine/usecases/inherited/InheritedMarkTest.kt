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

import uk.org.jurg.yakl.engine.utils.toIntArray
import uk.org.jurg.yakl.engine.v2.exceptions.Mark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InheritedMarkTest : InheritedImportTest() {
    @Test
    fun `Test marks`() {
        val content = getResource("test_mark.marks")
        content.split("---\n").forEach { input ->
            var index = 0
            var line = 0
            var column = 0
            while (input[index] != '*') {
                if (input[index] != '\n') {
                    line += 1
                    column = 0
                } else {
                    column += 1
                }
                index += 1
            }
            val mark = Mark("testMarks", index, line, column, input.toIntArray(), index)
            val snippet = mark.createSnippet(2, 79)
            assertTrue(snippet.indexOf("\n") > -1, "Must only have one '\n'.")
            assertEquals(
                snippet.indexOf("\n"),
                snippet.lastIndexOf("\n"),
                "Must only have only one '\n'."
            )
            val lines = snippet.split("\n")
            val data = lines[0]
            val pointer = lines[1]
            assertTrue(data.length < 82, "Mark must be restricted: $data")
            val dataPosition = data.indexOf("*")
            val pointerPosition = pointer.indexOf("^")
            assertEquals(dataPosition, pointerPosition, "Pointer should coincide with '*':\n $snippet")
        }
    }
}
