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
package uk.org.jurg.yakl.engine.v2.parser

import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.events.Event.ID


/**
 * This interface represents an input stream of [Events][Event].
 *
 * The parser and the scanner form together the 'Parse' step in the loading
 * process.
 *
 * @see org.snakeyaml.engine.v2.events.Event
 */
interface Parser : Iterator<Event> {
    /**
     * Check if the next event is one of the given type.
     *
     * @param choice Event ID.
     * @return `true` if the next event can be assigned to a variable
     * of the given type. Returns `false` if no more events
     * are available.
     * @throws ParserException Thrown in case of malformed input.
     */
    fun checkEvent(choice: ID): Boolean

    /**
     * Return the next event, but do not delete it from the stream.
     *
     * @return The event that will be returned on the next call to
     * [.next]
     * @throws ParserException Thrown in case of malformed input.
     */
    fun peekEvent(): Event

    /**
     * Returns the next event.
     *
     * The event will be removed from the stream.
     *
     * @return the next parsed event
     * @throws ParserException Thrown in case of malformed input.
     */
    override fun next(): Event
}
