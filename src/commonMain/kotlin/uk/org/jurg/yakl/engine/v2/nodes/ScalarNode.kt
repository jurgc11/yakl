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

import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.exceptions.Mark

/**
 * Represents a scalar node.
 *
 * Scalar nodes form the leaves in the node graph.
 */
class ScalarNode(
    tag: Tag,
    resolved: Boolean = true,
    /**
     * Value of this scalar.
     *
     * @return Scalar's value.
     */
    val value: String,
    /**
     * Get scalar style of this node.
     *
     * @return style of this scalar node
     * @see org.snakeyaml.engine.v2.events.ScalarEvent
     * Flow  styles - https://yaml.org/spec/1.2/spec.html.id2786942
     * Block styles - https://yaml.org/spec/1.2/spec.html.id2793652
     */
    val style: ScalarStyle,
    startMark: Mark? = null,
    endMark: Mark? = null
) : Node(tag, startMark, endMark) {

    init {
        this.resolved = resolved
    }

    override val nodeType: NodeType
        get() = NodeType.SCALAR

    override fun toString(): String {
        return "<${this::class.qualifiedName.toString()} (tag=$tag, value=$value)>"
    }

    val isPlain: Boolean
        get() = style === ScalarStyle.PLAIN
}
