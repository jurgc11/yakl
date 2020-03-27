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
 * Base class for the two collection types [mapping][MappingNode] and
 * [collection][SequenceNode].
 */
abstract class CollectionNode<T>(
    tag: Tag,
    /**
     * Serialization style of this collection.
     *
     * @return `true` for flow style, `false` for block
     * style.
     */
    var flowStyle: FlowStyle,
    startMark: Mark?,
    endMark: Mark?
) : Node(tag, startMark, endMark) {

    /**
     * Returns the elements in this sequence.
     *
     * @return Nodes in the specified order.
     */
    abstract val value: List<T>?

    fun setEndMark(endMark: Mark?) {
        this.endMark = endMark
    }

}
