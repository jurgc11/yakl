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
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.parser.ParserImpl
import uk.org.jurg.yakl.engine.v2.scanner.StreamReader


/**
 * Read the input stream and parse the content into events (opposite for Present or Emit)
 */
class Parse(private val settings: LoadSettings) {

    /**
     * Parse a YAML stream and produce parsing events.
     *
     * @param yaml - YAML document(s). Default encoding is UTF-8. The BOM must be present if the encoding is UTF-16 or UTF-32
     * @return parsed events
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun parseInputStream(yaml: Input): Iterable<Event> {
        return Iterable { ParserImpl(StreamReader(YamlUnicodeReader(yaml), settings), settings) }
    }

    /**
     * Parse a YAML stream and produce parsing events. Since the encoding is already known the BOM must not be present
     * (it will be parsed as content)
     *
     * @param yaml - YAML document(s).
     * @return parsed events
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun parseReader(yaml: Reader): Iterable<Event> {
        return Iterable { ParserImpl(StreamReader(yaml, settings), settings) }
    }

    /**
     * Parse a YAML stream and produce parsing events.
     *
     * @param yaml - YAML document(s). The BOM must not be present (it will be parsed as content)
     * @return parsed events
     * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
     */
    fun parseString(yaml: String): Iterable<Event> {

        //do not use lambda to keep Iterable and Iterator visible
        return object : Iterable<Event> {
            override fun iterator(): Iterator<Event> {
                return ParserImpl(StreamReader(StringReader(yaml), settings), settings)
            }
        }
    }

}
