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

package uk.org.jurg.yakl.engine.v2.api.lowlevel

import uk.org.jurg.yakl.engine.utils.StringWriter
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.emitter.Emitter
import uk.org.jurg.yakl.engine.v2.events.Event

/**
 * Emit the events into a data stream (opposite for Parse)
 */
class Present(private val settings: DumpSettings) {

    fun emitToString(events: Iterator<Event>): String {

        val writer = StringWriter()
        val emitter = Emitter(settings, writer)
        events.forEach { emitter.emit(it) }
        return writer.toString()
    }
}
