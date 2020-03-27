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
package uk.org.jurg.yakl.engine.usecases.references

import uk.org.jurg.yakl.engine.utils.getTimeMillis
import uk.org.jurg.yakl.engine.v2.api.Dump
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.api.Load
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReferencesTest {
    /**
     * Create data which is difficult to parse.
     *
     * @param size - size of the map, defines the complexity
     * @return YAML to parse
     */
    private fun createDump(size: Int): String {
        val root = HashMap<Any, Any>()
        var s1: HashMap<Any, Any>
        var s2: HashMap<Any, Any>
        var t1: HashMap<Any, Any>
        var t2: HashMap<Any, Any>
        s1 = root
        s2 = HashMap()
        /*
        the time to parse grows very quickly
        SIZE -> time to parse in seconds
        25 -> 1
        26 -> 2
        27 -> 3
        28 -> 8
        29 -> 13
        30 -> 28
        31 -> 52
        32 -> 113
        33 -> 245
        34 -> 500
         */
        for (i in 0 until size) {
            t1 = HashMap()
            t2 = HashMap()
            t1["foo"] = "1"
            t2["bar"] = "2"
            s1["a"] = t1
            s1["b"] = t2
            s2["a"] = t1
            s2["b"] = t2
            s1 = t1
            s2 = t2
        }

        // this is VERY BAD code
        // the map has itself as a key (no idea why it may be used except of a DoS attack)
        val f = HashMap<Any, Any>()
        f[f] = "a"
        f["g"] = root
        val dump = Dump(DumpSettings())
        return dump.dumpToString(f)
    }

    @Test
    @Ignore() //TODO
    fun referencesWithRecursiveKeysNotAllowedByDefault() {
        val output = createDump(30)
        //System.out.println(output);
        val time1: Long = getTimeMillis()
        // Load
        val settings = LoadSettings(
            maxAliasesForCollections = 150
        )

        val load = Load(settings)

        assertFailsWith<Exception>("Recursive key for mapping is detected but it is not configured to be allowed.")
        {
            load.loadFromString(output)
            println("Complete")
        }

        val time2: Long = getTimeMillis()
        val duration = (time2 - time1) / 1000.toFloat()
        assertTrue(duration < 1.0, "It should fail quickly. Time was $duration seconds.")
    }

    @Test
    @Ignore //TODO
    fun `Parsing with aliases may take a lot of time, CPU and memory`() {
        val output = createDump(25)
        // Load
        val time1: Long = getTimeMillis()
        val settings = LoadSettings(
            allowRecursiveKeys = true,
            maxAliasesForCollections = 50
        )
        val load = Load(settings)
        load.loadFromString(output)
        val time2: Long = getTimeMillis()
        val duration = (time2 - time1) / 1000.0
        assertTrue(duration > 0.9, "It should take time. Time was $duration seconds.")
        assertTrue(duration < 5.0, "Time was $duration seconds.")
    }

    @Test
    @Ignore //TODO
    fun `Prevent DoS attack by failing early`() {
        // without alias restriction this size should occupy tons of CPU, memory and time to parse
        val bigYAML = createDump(35)
        // Load
        val time1: Long = getTimeMillis()
        val settings = LoadSettings(
            allowRecursiveKeys = true,
            maxAliasesForCollections = 40
        )

        val load = Load(settings)
        assertFailsWith<Exception>("Number of aliases for non-scalar nodes exceeds the specified max=40")
        { load.loadFromString(bigYAML) }

        val time2: Long = getTimeMillis()
        val duration = (time2 - time1) / 1000.toFloat()
        assertTrue(duration < 1.0, "It should fail quickly. Time was $duration seconds.")
    }
}
