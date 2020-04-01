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

package uk.org.jurg.yakl.engine.utils

import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilesTest {
    @Test
    fun fileExists() {
        assertTrue(Files.fileExists("src/macosTest/resources/file.txt"))
        assertFalse(Files.fileExists("src/macosTest/resources/foo.txt"))
    }

    @Test
    fun isDirectory() {
        assertTrue(Files.isDirectory("src/macosTest/resources/directory"))
        assertFalse(Files.isDirectory("src/macosTest/resources/file.txt"))
    }

    @Test
    fun readFile() {
        assertEquals("Hello World!\n", Files.readFile("src/macosTest/resources/file.txt"))
        val ex = assertFailsWith<IOException> { Files.readFile("src/macosTest/resources/foo.txt") }
        assertEquals("Couldn't open file src/macosTest/resources/foo.txt", ex.message)
    }

    @Test
    fun listFiles() {
        assertEquals(listOf(".", "..", "file.txt", "directory"), Files.listFiles("src/macosTest/resources"))
        val ex = assertFailsWith<IOException> { Files.listFiles("src/macosTest/resources/file.txt") }
        assertEquals("Couldn't open directory src/macosTest/resources/file.txt", ex.message)
    }
}
