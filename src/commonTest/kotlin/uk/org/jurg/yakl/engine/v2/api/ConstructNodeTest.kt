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
package uk.org.jurg.yakl.engine.v2.api

import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import uk.org.jurg.yakl.engine.v2.nodes.SequenceNode
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConstructNodeTest {
    @Test
    fun failToConstructRecursive() {
        val constructNode: ConstructNode = object : ConstructNode {
            override fun construct(node: Node?): Any? {
                return null
            }
        }
        val node: Node = SequenceNode(
            tag = Tag.SEQ,
            value = listOf(ScalarNode(Tag.STR, value = "b", style = ScalarStyle.PLAIN)),
            flowStyle = FlowStyle.FLOW
        )
        node.recursive = true
        assertFailsWith(
            IllegalStateException::class,
            "Not implemented in uk.org.jurg.yakl.engine.v2.api.ConstructNodeTest$1"
        )
        { constructNode.constructRecursive(node, ArrayList<Any?>()) }


    }

    @Test
    fun failToConstructNonRecursive() {
        val constructNode: ConstructNode = object : ConstructNode {
            override fun construct(node: Node?): Any? {
                return null
            }
        }
        val node: Node = SequenceNode(
            tag = Tag.SEQ,
            value = listOf(ScalarNode(Tag.STR, value = "b", style = ScalarStyle.PLAIN)),
            flowStyle = FlowStyle.FLOW
        )
        node.recursive = false
        assertFailsWith(
            YamlEngineException::class,
            "Unexpected recursive structure for Node"
        ) { constructNode.constructRecursive(node, ArrayList<Any?>()) }

    }
}
