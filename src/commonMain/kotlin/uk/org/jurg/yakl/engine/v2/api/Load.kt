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

package uk.org.jurg.yakl.engine.v2.api

import kotlinx.io.Input
import uk.org.jurg.yakl.engine.utils.Reader
import uk.org.jurg.yakl.engine.utils.StringReader
import uk.org.jurg.yakl.engine.v2.composer.Composer
import uk.org.jurg.yakl.engine.v2.constructor.BaseConstructor
import uk.org.jurg.yakl.engine.v2.constructor.StandardConstructor
import uk.org.jurg.yakl.engine.v2.parser.ParserImpl
import uk.org.jurg.yakl.engine.v2.scanner.StreamReader

/**
 * Common way to load Java instance(s). This class is not thread-safe. Which means that all the methods of the same
 * instance can be called only by one thread.
 * It is better to create an instance for every YAML stream.
 */
class Load(
    private val settings: LoadSettings,
    private val constructor: BaseConstructor = StandardConstructor(settings)
) {

    private fun createComposer(streamReader: StreamReader): Composer {
        return Composer(ParserImpl(streamReader, settings), settings)
    }

    protected fun createComposer(yamlStream: Input): Composer {
        return createComposer(StreamReader(YamlUnicodeReader(yamlStream), settings))
    }

    protected fun createComposer(yaml: String): Composer {
        return createComposer(StreamReader(StringReader(yaml), settings))
    }

    protected fun createComposer(yamlReader: Reader): Composer {
        return createComposer(StreamReader(yamlReader, settings))
    }

    // Load  a single document
    protected fun loadOne(composer: Composer): Any? {
        val nodeOptional = composer.getSingleNode()
        return constructor.constructSingleDocument(nodeOptional)
    }

    /**
     * Parse the only YAML document in a stream and produce the corresponding
     * Java object.
     *
     * @param yamlStream - data to load from (BOM is respected to detect encoding and removed from the data)
     * @return parsed Java instance
     */
    fun loadFromInputStream(yamlStream: Input): Any? {
        return loadOne(createComposer(yamlStream))
    }

    /**
     * Parse a YAML document and create a Java instance
     *
     * @param yamlReader - data to load from (BOM must not be present)
     * @return parsed Java instance
     */
    fun loadFromReader(yamlReader: Reader): Any? {
        return loadOne(createComposer(yamlReader))
    }

    /**
     * Parse a YAML document and create a Java instance
     *
     * @param yaml - YAML data to load from (BOM must not be present)
     * @return parsed Java instance
     * @throws org.snakeyaml.engine.v2.exceptions.YamlEngineException if the YAML is not valid
     */
    fun loadFromString(yaml: String): Any? {
        return loadOne(createComposer(yaml))
    }

    // Load all the documents
    private fun loadAll(composer: Composer): Iterable<Any?> {
        val result:Iterator<Any?> = YamlIterator(composer, constructor)
        return YamlIterable(result)
    }

    /**
     * Parse all YAML documents in a stream and produce corresponding Java
     * objects. The documents are parsed only when the iterator is invoked.
     *
     * @param yamlStream - YAML data to load from (BOM is respected to detect encoding and removed from the data)
     * @return an Iterable over the parsed Java objects in this stream in proper sequence
     */
    fun loadAllFromInputStream(yamlStream: Input): Iterable<Any?> {
        val composer: Composer = createComposer(StreamReader(
            YamlUnicodeReader(
                yamlStream
            ), settings))
        return loadAll(composer)
    }

    /**
     * Parse all YAML documents in a String and produce corresponding Java
     * objects. The documents are parsed only when the iterator is invoked.
     *
     * @param yamlReader - YAML data to load from (BOM must not be present)
     * @return an Iterable over the parsed Java objects in this stream in proper sequence
     */
    fun loadAllFromReader(yamlReader: Reader): Iterable<Any?> {
        val composer: Composer = createComposer(StreamReader(yamlReader, settings))
        return loadAll(composer)
    }

    /**
     * Parse all YAML documents in a String and produce corresponding Java
     * objects. (Because the encoding in known BOM is not respected.) The
     * documents are parsed only when the iterator is invoked.
     *
     * @param yaml - YAML data to load from (BOM must not be present)
     * @return an Iterable over the parsed Java objects in this stream in proper sequence
     */
    fun loadAllFromString(yaml: String): Iterable<Any?> {
        val composer: Composer = createComposer(StreamReader(StringReader(yaml), settings))
        return loadAll(composer)
    }

    private class YamlIterable(private val iterator: Iterator<Any?>) : Iterable<Any?> {

        override fun iterator(): Iterator<Any?> {
            return iterator
        }

    }

    private class YamlIterator(
        private val composer: Composer,
        private val constructor: BaseConstructor
    ) : Iterator<Any?> {

        override fun hasNext(): Boolean {
            return composer.hasNext()
        }

        override fun next(): Any? {
            val node = composer.next()
            return constructor.constructSingleDocument(node)
        }

    }

}
