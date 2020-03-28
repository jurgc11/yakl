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
package uk.org.jurg.yakl.engine.v2.nodes

import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.exceptions.Mark

/**
 * Represents a sequence.
 *
 * A sequence is a ordered collection of nodes.
 */
class SequenceNode(
    tag: Tag,
    resolved: Boolean = true,
    /**
     * Returns the elements in this sequence.
     *
     * @return Nodes in the specified order.
     */
    override val value: List<Node>,
    flowStyle: FlowStyle,
    startMark: Mark? = null,
    endMark: Mark? = null
) : CollectionNode<Node?>(
    tag,
    flowStyle,
    startMark,
    endMark
) {
    init {
        this.resolved = resolved
    }

    override val nodeType: NodeType
        get() = NodeType.SEQUENCE

    override fun toString(): String {
        return "<${this::class.qualifiedName} (tag=$tag, value=$value)>"
    }
}
