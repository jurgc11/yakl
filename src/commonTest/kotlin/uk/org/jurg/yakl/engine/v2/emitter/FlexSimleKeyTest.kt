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
package uk.org.jurg.yakl.engine.v2.emitter

import uk.org.jurg.yakl.engine.v2.api.Dump
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlexSimleKeyTest {
    private val len = 130

    @Test
    fun testLongKey() {
        val dump = Dump(createOptions(len))
        val root = HashMap<String, Any>()
        val map = HashMap<String, String>()
        val key = createKey(len)
        map[key] = "v1"
        root["data"] = map
        assertEquals("data: {? $key\n  : v1}\n", dump.dumpToString(root))
    }

    @Test
    fun testForceLongKeyToBeImplicit() {
        val dump = Dump(createOptions(len + 10))
        val root = HashMap<String, Any>()
        val map = HashMap<String, String>()
        val key = createKey(len)
        map[key] = "v1"
        root["data"] = map
        assertEquals("data: {$key: v1}\n", dump.dumpToString(root))
    }

    @Test
    fun testTooLongKeyLength() {
        assertFailsWith(
            YamlEngineException::class, "The simple key must not span more than 1024 stream " +
                    "characters. See https://yaml.org/spec/1.2/spec.html#id2798057"
        ) { createOptions(1024 + 1) }
    }

    private fun createOptions(len: Int): DumpSettings {
        return DumpSettings(maxSimpleKeyLength = len)
    }

    private fun createKey(length: Int): String {
        val outputBuffer = StringBuilder(length)
        for (i in 0 until length) {
            outputBuffer.append("" + (i + 1) % 10)
        }
        val prefix: String = length.toString()
        val result =
            prefix + "_" + outputBuffer.toString().substring(0, length - prefix.length - 1)
        if (result.length != length) throw RuntimeException("It was: " + result.length)
        return result
    }
}
