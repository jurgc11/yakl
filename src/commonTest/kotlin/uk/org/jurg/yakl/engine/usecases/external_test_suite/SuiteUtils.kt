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
package uk.org.jurg.yakl.engine.usecases.external_test_suite

import kotlinx.io.IOException
import uk.org.jurg.yakl.engine.utils.Files
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Parse
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException

object SuiteUtils {
    val deviationsWithSuccess = listOf("9C9N", "SU5Z", "QB6E", "QLJ7", "EB22")
    val deviationsWithError = listOf(
        "CXX2",
        "KZN9",
        "DC7X",
        "6HB6",
        "2JQS",
        "6M2F",
        "S3PD",
        "Q5MG",
        "FRK4",
        "NHX8",
        "DBG4",
        "4ABK",
        "M7A3",
        "9MMW",
        "6BCT",
        "A2M4",
        "2SXE",
        "DK3J",
        "W5VH",
        "8XYN",
        "K54U",
        "HS5T",
        "UT92",
        "W4TN",
        "FP8R",
        "WZ62",
        "7Z25"
    )
    private val FOLDER_NAME = "src/commonTest/resources/comprehensive-test-suite-data"

    fun getAllFoldersIn(folder: String): List<String> {

        if (!Files.fileExists(folder)) {
            throw RuntimeException("Folder not found: $folder")
        }
        if (!Files.isDirectory(folder)) {
            throw RuntimeException("Must be folder: $folder")
        }
        return Files.listFiles(folder)
            .filter { Files.isDirectory("$folder/$it") }
            .filter { !it.startsWith('.') }
            .toList()
    }

    private fun readData(path: String, file: String): SuiteData {

        val base = "$path/$file"
        return try {

            val label: String = Files.readFile("$base/===")
            val input: String = Files.readFile("$base/in.yaml")
            val eventContents = Files.readFile("$base/test.event").lines()
            val events = eventContents

                .filter { it.isNotEmpty() }

            val error = Files.fileExists("$base/error")
            SuiteData(file, label, input, events, error)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun getAll(): List<SuiteData> {
        return getAllFoldersIn(FOLDER_NAME).map { file -> readData(FOLDER_NAME, file) }
    }

    fun getOne(name: String): SuiteData {
        return readData(FOLDER_NAME, name)
    }

    fun parseData(data: SuiteData): ParseResult? {
        val settings = LoadSettings(
            label = data.label
        )

        return try {
            ParseResult(events = Parse(settings).parseString(data.input).toList())
        } catch (e: YamlEngineException) {
            ParseResult(error = e)
        }
    }
}

class ParseResult(
    val events: List<Event> = listOf(),
    val error: Exception? = null
)
