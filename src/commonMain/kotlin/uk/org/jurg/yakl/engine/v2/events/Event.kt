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

import uk.org.jurg.yakl.engine.v2.exceptions.Mark

/**
 * Basic unit of output from a [org.snakeyaml.engine.v2.parser.Parser] or input
 * of a [org.snakeyaml.engine.v2.emitter.Emitter].
 */
abstract class Event (
    val startMark: Mark? = null,
    val endMark: Mark? = null
) {
    enum class ID {
        Alias, DocumentEnd, DocumentStart, MappingEnd, MappingStart, Scalar, SequenceEnd, SequenceStart, StreamEnd, StreamStart //NOSONAR
    }

    /**
     * Get the type (kind) if this Event
     *
     * @return the ID of this Event
     */
    abstract val eventId: ID

    /*
     * Create Node for emitter
     */
    init {
        if (startMark == null && endMark != null || startMark != null && endMark == null) {
            throw NullPointerException("Both marks must be either present or absent.")
        }
    }
}
