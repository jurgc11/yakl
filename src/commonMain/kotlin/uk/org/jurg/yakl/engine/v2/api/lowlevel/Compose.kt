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

import kotlinx.io.Input
import uk.org.jurg.yakl.engine.utils.Reader
import uk.org.jurg.yakl.engine.utils.StringReader
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.api.YamlUnicodeReader
import uk.org.jurg.yakl.engine.v2.composer.Composer
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.parser.ParserImpl
import uk.org.jurg.yakl.engine.v2.scanner.StreamReader


class Compose(private val settings: LoadSettings) {

    /**
     * Parse a YAML stream and produce [Node]
     *
     * @param yaml - YAML document(s). Since the encoding is already known the BOM must not be present (it will be parsed as content)
     * @return parsed [Node] if available
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun composeReader(yaml: Reader): Node? {
        return Composer(
            ParserImpl(StreamReader(yaml, settings), settings),
            settings
        ).getSingleNode()
    }

    /**
     * Parse a YAML stream and produce [Node]
     *
     * @param yaml - YAML document(s). Default encoding is UTF-8. The BOM must be present if the encoding is UTF-16 or UTF-32
     * @return parsed [Node] if available
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun composeInputStream(yaml: Input): Node? {
        return Composer(
            ParserImpl(StreamReader(YamlUnicodeReader(yaml), settings), settings),
            settings
        ).getSingleNode()
    }

    /**
     * Parse a YAML stream and produce [Node]
     *
     * @param yaml - YAML document(s).
     * @return parsed [Node] if available
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun composeString(yaml: String): Node? {
        return Composer(
            ParserImpl(StreamReader(StringReader(yaml), settings), settings),
            settings
        ).getSingleNode()
    }

    // Compose all documents
    /**
     * Parse all YAML documents in a stream and produce corresponding representation trees.
     *
     * @param yaml stream of YAML documents
     * @return parsed root Nodes for all the specified YAML documents
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun composeAllFromReader(yaml: Reader): Iterable<Node> {
        return Iterable {
            Composer(
                ParserImpl(
                    StreamReader(yaml, settings),
                    settings
                ), settings
            )
        }
    }

    /**
     * Parse all YAML documents in a stream and produce corresponding representation trees.
     *
     * @param yaml - YAML document(s). Default encoding is UTF-8. The BOM must be present if the encoding is UTF-16 or UTF-32
     * @return parsed root Nodes for all the specified YAML documents
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun composeAllFromInputStream(yaml: Input): Iterable<Node> {
        return Iterable {
            Composer(
                ParserImpl(
                    StreamReader(YamlUnicodeReader(yaml), settings),
                    settings
                ), settings
            )
        }
    }

    /**
     * Parse all YAML documents in a stream and produce corresponding representation trees.
     *
     * @param yaml - YAML document(s).
     * @return parsed root Nodes for all the specified YAML documents
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun composeAllFromString(yaml: String): Iterable<Node> {
        //do not use lambda to keep Iterable and Iterator visible
        return object : Iterable<Node> {

            override fun iterator(): Iterator<Node> {
                return Composer(
                    ParserImpl(
                        StreamReader(StringReader(yaml), settings), settings
                    ), settings
                )
            }
        }
    }
}
