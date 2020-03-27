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

package uk.org.jurg.yakl.engine.usecases.external_test_suite

import uk.org.jurg.yakl.engine.usecases.external_test_suite.SuiteUtils.getOne
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Compose
import uk.org.jurg.yakl.engine.v2.api.lowlevel.Serialize
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComposeSuiteTest {
    private val emptyNodes: List<String> = listOf("AVM7", "8G76", "98YD")

    private val allValid: List<SuiteData> = SuiteUtils.getAll()
        .filter { !it.error }
        .filter { !SuiteUtils.deviationsWithSuccess.contains(it.name) }
        .filter { !SuiteUtils.deviationsWithError.contains(it.name) }

    private val allValidAndNonEmpty: List<SuiteData> = allValid
        .filter { !emptyNodes.contains(it.name) }

    private val allValidAndEmpty: List<SuiteData> = allValid
        .filter { emptyNodes.contains(it.name) }

    @Test
    fun `Compose run one test`() {
        val data = getOne("C4HZ")
        val settings = LoadSettings(
            label = data.label
        )
        val node = Compose(settings).composeString(data.input)
        assertNotNull(node)
        //        System.out.println(node);
    }

    @Test
    fun `Compose run comprehensive test suite for non empty Nodes`() {
        for (data in allValidAndNonEmpty) {
            val result = composeData(data)
            val nodes = result.node
            assertFalse(nodes.isEmpty(), "${data.name} -> ${data.label}\n${data.input}")
            val settings = DumpSettings(
                explicitStart = true,
                explicitEnd = true
            )
            val serialize = Serialize(settings)
            val events = serialize.serializeAll(nodes)
            assertEquals(data.events.size, events.size, "${data.name} -> ${data.label}\n${data.input}")
            for (i in events.indices) {
                val event = events[i]
                val representation = EventRepresentation(event)
                val etalon = data.events[i]
                assertTrue(
                    representation.isSameAs(etalon),
                    """${data.name} -> ${data.label}
                    |${data.input}
                    |${data.events[i]}
                    |${events[i]}
                    |""".trimMargin()
                )
            }
        }
    }

    @Test
    fun `Compose run comprehensive test suite for empty Nodes`() {
        allValidAndEmpty.forEach { data ->
            val result = composeData(data)
            val nodes = result.node
            assertTrue(nodes.isEmpty(), "${data.name} -> ${data.label}\n${data.input}")
        }
    }

    private fun composeData(data: SuiteData): ComposeResult {
        return try {
            val settings = LoadSettings(
                label = data.label
            )
            val list = Compose(settings).composeAllFromString(data.input).toList()
            ComposeResult(node = list)
        } catch (e: YamlEngineException) {
            ComposeResult(error = e)
        }
    }
}

class ComposeResult(
    val node: List<Node> = listOf(),
    val error: Exception? = null
)
