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

package uk.org.jurg.yakl.engine.v2.events

import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.exceptions.Mark

/**
 * Marks the beginning of a mapping node.
 *
 *
 * This event is followed by a number of key value pairs. <br></br>
 * The pairs are not in any particular order. However, the value always directly
 * follows the corresponding key. <br></br>
 * After the key value pairs follows a [MappingEndEvent].
 *
 *
 *
 * There must be an even number of node events between the start and end event.
 *
 *
 * @see MappingEndEvent
 */
class MappingStartEvent(
    anchor: Anchor?,
    tag: String?,
    implicit: Boolean,
    flowStyle: FlowStyle,
    startMark: Mark? = null,
    endMark: Mark? = null
) : CollectionStartEvent(anchor, tag, implicit, flowStyle, startMark, endMark) {

    override val eventId: ID
        get() = ID.MappingStart

    override fun toString(): String {
        val builder = StringBuilder("+MAP")
        if (flowStyle === FlowStyle.FLOW) {
            //TODO builder.append(" {}") is better visually, but the tests data do not use it yet
        }
        builder.append(super.toString())
        return builder.toString()
    }
}
