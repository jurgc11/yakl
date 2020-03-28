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
package uk.org.jurg.yakl.engine.v2.api.lowlevel

import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.emitter.Emitable
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.serializer.Serializer

class Serialize(private val settings: DumpSettings) {

    /**
     * Serialize a [Node] and produce events.
     *
     * @param node - [Node] to serialize
     * @return serialized events
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun serializeOne(node: Node): List<Event> {
        return serializeAll(listOf(node))
    }

    /**
     * Serialize [Node]s and produce events.
     *
     * @param nodes - [Node]s to serialize
     * @return serialized events
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun serializeAll(nodes: List<Node>): List<Event> {

        val emitableEvents = EmitableEvents()
        val serializer = Serializer(settings, emitableEvents)
        serializer.open()
        nodes.forEach { serializer.serialize(it) }
        serializer.close()
        return emitableEvents.events
    }
}

internal class EmitableEvents : Emitable {
    val events = ArrayList<Event>()

    override fun emit(event: Event) {
        events.add(event)
    }

}
