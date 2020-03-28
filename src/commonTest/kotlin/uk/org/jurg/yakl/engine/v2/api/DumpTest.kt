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
package uk.org.jurg.yakl.engine.v2.api

import uk.org.jurg.yakl.engine.utils.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class DumpTest {
    @Test
    fun `Dump string`() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val str = dump.dumpToString("a")
        assertEquals("a\n", str)
    }

    @Test
    fun `Dump int`() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val str = dump.dumpToString(1)
        assertEquals("1\n", str)
    }

    @Test
    fun `Dump boolean`() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val str = dump.dumpToString(true)
        assertEquals("true\n", str)
    }

    @Test
    fun `Dump seq`() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val str = dump.dumpToString(listOf(2, "a", true))
        assertEquals("[2, a, true]\n", str)
    }

    @Test
    fun `Dump map`() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val output = dump.dumpToString(mapOf("x" to 1, "y" to 2, "z" to 3))
        assertEquals("{x: 1, y: 2, z: 3}\n", output)
    }

    @Test
    fun `Dump all instances`() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val streamToStringWriter = StringWriter()
        val list = mutableListOf("a", null, true)
        dump.dumpAll(list.iterator(), streamToStringWriter)
        assertEquals(
            """
                a
                --- null
                --- true

                """.trimIndent(), streamToStringWriter.toString()
        )
        //load back
        val loadSettings = LoadSettings()
        val load = Load(loadSettings)
        for (obj in load.loadAllFromString(streamToStringWriter.toString())) {
            assertEquals(list.removeAt(0), obj)
        }
    }

    @Test
    fun `Dump all instances to string`() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val list = mutableListOf("a", null, true)
        val output = dump.dumpAllToString(list.iterator())
        assertEquals(
            """
                a
                --- null
                --- true

                """.trimIndent(), output
        )
        //load back
        val loadSettings = LoadSettings()
        val load = Load(loadSettings)
        for (obj in load.loadAllFromString(output)) {
            assertEquals(list.removeAt(0), obj)
        }
    }

//    @Test
//    fun `Dump to File`() {
//        val settings = DumpSettings()
//        val dump = Dump(settings)
//        val file = "target/temp.yaml"
//        Files.deleteFile(file)
//
//        assertFalse(Files.fileExists(file))
//        file.createNewFile()
//        val writer: Writer = object : OutputWriter(
//            FileOutputStream(file),
//            StandardCharsets.UTF_8
//        ) {
//
//            override fun processIOException(e: IOException?) {
//                throw RuntimeException(e)
//            }
//        }
//        dump.dump(mapOf("x" to 1, "y" to 2, "z" to 3), writer)
//        assertTrue(file.exists())
//        file.delete() //on Windows the file is not deleted
//    }
}
