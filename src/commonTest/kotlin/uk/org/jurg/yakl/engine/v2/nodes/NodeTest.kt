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
package uk.org.jurg.yakl.engine.v2.nodes

import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

//@org.junit.jupiter.api.Tag("fast")
 class NodeTest {
    @Test
    fun notEqualToTheSameNode() {
        val node1 = ScalarNode(tag = Tag.STR, value = "a", style = ScalarStyle.PLAIN)
        val node2 = ScalarNode(tag = Tag.STR, value = "a", style = ScalarStyle.PLAIN)
        assertNotEquals(node1, node2, "Nodes with the same contant are not equal")
        assertNotEquals(node2, node1, "Nodes with the same contant are not equal")
    }

    @Test
    fun equalsToItself() {
        val node = ScalarNode(tag = Tag.STR, value = "a", style = ScalarStyle.PLAIN)
        assertEquals(node, node)
    }

    @Test
    fun properties() {
        val node = ScalarNode(tag = Tag.STR, value = "a", style = ScalarStyle.PLAIN)
        assertNull(node.getProperty("p"))
        assertNull(node.setProperty("p", "value"))
        assertEquals("value", node.getProperty("p"))
    }
}
