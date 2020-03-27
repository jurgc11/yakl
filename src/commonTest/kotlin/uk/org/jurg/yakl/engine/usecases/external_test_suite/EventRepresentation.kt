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
package uk.org.jurg.yakl.engine.usecases.external_test_suite

import uk.org.jurg.yakl.engine.v2.events.AliasEvent
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.events.MappingStartEvent
import uk.org.jurg.yakl.engine.v2.events.NodeEvent
import uk.org.jurg.yakl.engine.v2.events.ScalarEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceStartEvent
import uk.org.jurg.yakl.engine.v2.nodes.Tag

/**
 * Event representation for the external test suite
 */
class EventRepresentation(private val event: Event) {
    val representation: String
        get() = event.toString()

    fun isSameAs(data: String): Boolean {
        val split = data.split(' ')
        if (!event.toString().startsWith(split[0])) return false
        /*
        if (event instanceof DocumentStartEvent) {
            DocumentStartEvent e = (DocumentStartEvent) event;
            if (e.isExplicit()) {
                if (split.size() != 2 || !split.get(1).equals("---")) return false;
            } else {
                if (split.size() != 1) return false;
            }
        }
        if (event instanceof DocumentEndEvent) {
            DocumentEndEvent e = (DocumentEndEvent) event;
            if (e.isExplicit()) {
                if (split.size() != 2 || !split.get(1).equals("...")) return false;
            } else {
                if (split.size() != 1) return false;
            }
        }
        */
        if (event is MappingStartEvent) {
            if (event.tag != null && Tag.MAP.value != event.tag) {
                val last = split[split.size - 1]
                if (last != "<${event.tag.toString()}>") return false
            }
        } else if (event is SequenceStartEvent) {
            if (event.tag != null && Tag.SEQ.value != event.tag) {
                val last = split[split.size - 1]
                if (last != "<${event.tag.toString()}>") return false
            }
        }
        if (event is NodeEvent) {
            if (event.anchor != null) {
                if (event is AliasEvent) {
                    if (!split[1].startsWith("*")) return false
                } else {
                    if (!split[1].startsWith("&")) return false
                }
            }
        }
        if (event is ScalarEvent) {
            val tag = event.tag
            if (tag != null) {
                val implicit = event.implicit
                if (implicit.bothFalse()) {
                    if (!data.contains("<$tag>")) return false
                }
            }
            val end = event.scalarStyle.toString() + event.escapedValue()
            return data.endsWith(end)
        }
        return true
    }

}
