/**
 * Copyright (c) 2018, http://www.snakeyaml.org
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
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
 * Marks the end of a document.
 *
 *
 * This event follows the document's content.
 *
 */
class DocumentEndEvent (
    val explicit: Boolean,
    startMark: Mark? = null,
    endMark: Mark? = null
) : Event(startMark, endMark) {

    override val eventId: ID
        get() = ID.DocumentEnd

    override fun toString(): String {
        val builder = StringBuilder("-DOC")
        if (explicit) {
            builder.append(" ...")
        }
        return builder.toString()
    }
}
