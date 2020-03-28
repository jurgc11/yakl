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
package uk.org.jurg.yakl.engine.usecases.inherited

import kotlinx.io.Input
import uk.org.jurg.yakl.engine.utils.Files
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.parser.ParserImpl
import uk.org.jurg.yakl.engine.v2.scanner.StreamReader
import uk.org.jurg.yakl.engine.v2.utils.TestUtils
import kotlin.test.assertTrue

const val PATH = "inherited_yaml_1_1"

abstract class InheritedImportTest {

    protected fun getResource(theName: String): String {
        return TestUtils.getResource("/$PATH/$theName")
    }

    protected fun getStreamsByExtension(extension: String): List<String> {
        return getStreamsByExtension(extension, false)
    }

    protected fun getStreamsByExtension(extension: String, onlyIfCanonicalPresent: Boolean): List<String> {
        val file = "src/commonTest/resources/$PATH"
        assertTrue(Files.fileExists(file), "Folder not found: $file")
        assertTrue(Files.isDirectory(file))
        return Files.listFiles(file)
            .filter { fileFilter(file, it, extension, onlyIfCanonicalPresent) }
            .map { "$file/$it" }
    }

    private fun fileFilter(dir: String, name: String, extension: String, onlyIfCanonicalPresent: Boolean): Boolean {
        val position = name.lastIndexOf('.')
        val canonicalFileName = name.substring(0, position) + ".canonical"
        return if (onlyIfCanonicalPresent && !Files.fileExists("$dir/$canonicalFileName")) {
            false
        } else {
            name.endsWith(extension)
        }
    }

    protected fun getFileByName(name: String): String {
        val file = "src/test/resources/$PATH/$name"
        assertTrue(Files.fileExists(file), "Folder not found: $file")
        assertTrue(Files.isDirectory(file))
        return file
    }

    protected fun canonicalParse(input2: String, label: String): List<Event> {
        val parser = CanonicalParser(input2, label)
        return parser.asSequence().toList()
    }

    protected fun parse(input: Input): List<Event> {
        val settings = LoadSettings()
        val reader = StreamReader(YamlUnicodeReader(input), settings)
        val parser = ParserImpl(reader, settings)
        val result = parser.asSequence().toList()
        input.close()
        return result
    }

}
