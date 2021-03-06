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

import uk.org.jurg.yakl.engine.v2.exceptions.Mark

/**
 * Marks the end of a stream that might have contained multiple documents.
 *
 *
 * This event is the last event that a parser emits. Together with
 * [StreamStartEvent] (which is the first event a parser emits) they mark
 * the beginning and the end of a stream of documents.
 *
 *
 *
 * See [Event] for an exemplary output.
 *
 */
class StreamEndEvent(
    startMark: Mark? = null,
    endMark: Mark? = null
) : Event(startMark, endMark) {

    override val eventId: ID
        get() = ID.StreamEnd

    override fun toString(): String {
        return "-STR"
    }
}
