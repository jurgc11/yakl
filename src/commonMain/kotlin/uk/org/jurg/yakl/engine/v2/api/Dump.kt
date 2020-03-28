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
package uk.org.jurg.yakl.engine.v2.api

import uk.org.jurg.yakl.engine.utils.StringWriter
import uk.org.jurg.yakl.engine.utils.Writer
import uk.org.jurg.yakl.engine.v2.emitter.Emitter
import uk.org.jurg.yakl.engine.v2.representer.BaseRepresenter
import uk.org.jurg.yakl.engine.v2.representer.StandardRepresenter
import uk.org.jurg.yakl.engine.v2.serializer.Serializer

class Dump(
    private val settings: DumpSettings,
    private val representer: BaseRepresenter = StandardRepresenter(settings)
) {

    /**
     * Dump all the instances from the iterator into a stream with every instance in a separate YAML document
     *
     * @param instancesIterator - instances to serialize
     * @param writer  - destination I/O writer
     */
    fun dumpAll(instancesIterator: Iterator<Any?>, writer: Writer) {
        val serializer = Serializer(settings, Emitter(settings, writer))
        serializer.open()
        instancesIterator.forEach {
            val node = representer.represent(it)
            serializer.serialize(node)
        }
        serializer.close()
    }

    /**
     * Dump a single instance into a YAML document
     *
     * @param yaml             - instance to serialize
     * @param writer - destination I/O writer
     */
    fun dump(yaml: Any?, writer: Writer) {
        val iter = setOf(yaml).iterator()
        dumpAll(iter, writer)
    }

    /**
     * Dump all the instances from the iterator into a stream with every instance in a separate YAML document
     *
     * @param instancesIterator - instances to serialize
     * @return String representation of the YAML stream
     */
    fun dumpAllToString(instancesIterator: Iterator<Any?>): String {
        val writer = StringWriter()
        dumpAll(instancesIterator, writer)
        return writer.toString()
    }

    /**
     * Dump all the instances from the iterator into a stream with every instance in a separate YAML document
     *
     * @param yaml - instance to serialize
     * @return String representation of the YAML stream
     */
    fun dumpToString(yaml: Any?): String {
        val writer = StringWriter()
        dump(yaml, writer)
        return writer.toString()
    }

}
