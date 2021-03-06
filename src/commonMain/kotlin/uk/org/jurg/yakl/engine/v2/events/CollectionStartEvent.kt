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
package uk.org.jurg.yakl.engine.v2.events

import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.exceptions.Mark

abstract class CollectionStartEvent(
    anchor: Anchor?,
    val tag: String?,
    val implicit: Boolean,
    val flowStyle: FlowStyle,
    startMark: Mark?,
    endMark: Mark?
) : NodeEvent(anchor, startMark, endMark) {

    open fun isFlow(): Boolean {
        return FlowStyle.FLOW === flowStyle
    }

    override fun toString(): String {
        val builder = StringBuilder()
        if (anchor != null) {
            builder.append(" &").append(anchor)
        }
        if (!implicit && tag != null) {
            builder.append(" <").append(tag).append(">")
        }
        return builder.toString()
    }
}
