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

package uk.org.jurg.yakl.engine.v2.nodes

import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.exceptions.Mark

/**
 * Represents a map.
 *
 *
 * A map is a collection of unsorted key-value pairs.
 *
 */
class MappingNode(
    tag: Tag,
    resolved: Boolean = true,
    override var value: MutableList<NodeTuple>,
    flowStyle: FlowStyle,
    startMark: Mark? = null,
    endMark: Mark? = null
) : CollectionNode<NodeTuple?>(tag, flowStyle, startMark, endMark) {

    /**
     * true if map contains merge node
     */
    var merged = false

    override val nodeType: NodeType
        get() = NodeType.MAPPING

    override fun toString(): String {
        val values: String
        val buf = StringBuilder()
        for (node in value) {
            buf.append("{ key=")
            buf.append(node.keyNode)
            buf.append("; value=")
            if (node.valueNode is CollectionNode<*>) { // to avoid overflow in case of recursive structures
                buf.append((node.valueNode as Any).hashCode())
            } else {
                buf.append(node.toString())
            }
            buf.append(" }")
        }
        values = buf.toString()
        return "<" + this::class.qualifiedName.toString() + " (tag=" + tag.toString() + ", values=" + values + ")>"
    }

    init {
        this.resolved = resolved
    }
}
