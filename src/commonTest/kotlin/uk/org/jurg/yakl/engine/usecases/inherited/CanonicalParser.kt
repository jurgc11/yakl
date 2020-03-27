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


package uk.org.jurg.yakl.engine.usecases.inherited

import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.common.SpecVersion
import uk.org.jurg.yakl.engine.v2.events.AliasEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentEndEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentStartEvent
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.events.ImplicitTuple
import uk.org.jurg.yakl.engine.v2.events.MappingEndEvent
import uk.org.jurg.yakl.engine.v2.events.MappingStartEvent
import uk.org.jurg.yakl.engine.v2.events.ScalarEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceEndEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceStartEvent
import uk.org.jurg.yakl.engine.v2.events.StreamEndEvent
import uk.org.jurg.yakl.engine.v2.events.StreamStartEvent
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.parser.Parser
import uk.org.jurg.yakl.engine.v2.tokens.AliasToken
import uk.org.jurg.yakl.engine.v2.tokens.AnchorToken
import uk.org.jurg.yakl.engine.v2.tokens.ScalarToken
import uk.org.jurg.yakl.engine.v2.tokens.TagToken
import uk.org.jurg.yakl.engine.v2.tokens.Token

class CanonicalParser(data: String, private val label: String) : Parser, Iterator<Event> {
    private val events = mutableListOf<Event?>()
    private var parsed: Boolean = false
    private val scanner = CanonicalScanner(data, label)

    // stream: STREAM-START document* STREAM-END
    private fun parseStream() {
        scanner.getToken(Token.ID.StreamStart)
        events.add(StreamStartEvent(null, null))
        while (!scanner.checkToken(Token.ID.StreamEnd)) {
            if (scanner.checkToken(Token.ID.Directive, Token.ID.DocumentStart)) {
                parseDocument()
            } else {
                throw CanonicalException("document is expected, got ${scanner.tokens[0]} in $label")
            }
        }
        scanner.getToken(Token.ID.StreamEnd)
        events.add(StreamEndEvent(null, null))
    }

    // document: DIRECTIVE? DOCUMENT-START node
    private fun parseDocument() {
        if (scanner.checkToken(Token.ID.Directive)) {
            scanner.getToken(Token.ID.Directive)
        }
        scanner.getToken(Token.ID.DocumentStart)
        events.add(
            DocumentStartEvent(true, SpecVersion(1, 2), mapOf(), null, null)
        )
        parseNode()
        if (scanner.checkToken(Token.ID.DocumentEnd)) {
            scanner.getToken(Token.ID.DocumentEnd)
        }
        events.add(DocumentEndEvent(true, null, null))
    }

    // node: ALIAS | ANCHOR? TAG? (SCALAR|sequence|mapping)
    private fun parseNode() {
        if (scanner.checkToken(Token.ID.Alias)) {
            val token = scanner.next() as AliasToken
            events.add(AliasEvent(token.value, null, null))
        } else {
            val anchor = if (scanner.checkToken(Token.ID.Anchor)) {
                val token = scanner.next() as AnchorToken
                token.value
            } else {
                null
            }
            val tag = if (scanner.checkToken(Token.ID.Tag)) {
                val token = scanner.next() as TagToken
                token.value.handle + token.value.suffix
            } else {
                null
            }
            when {
                scanner.checkToken(Token.ID.Scalar) -> {
                    val token = scanner.next() as ScalarToken
                    val tuple =  ImplicitTuple(plain = false, nonPlain = false)
                    events.add(ScalarEvent(anchor, tag, tuple, token.value, ScalarStyle.PLAIN))
                }
                scanner.checkToken(Token.ID.FlowSequenceStart) -> {
                    events.add(SequenceStartEvent(anchor, Tag.SEQ.value, false, FlowStyle.AUTO))
                    parseSequence()
                }
                scanner.checkToken(Token.ID.FlowMappingStart) -> {
                    events.add(MappingStartEvent(anchor, Tag.MAP.value, false, FlowStyle.AUTO))
                    parseMapping()
                }
                else -> {
                    throw CanonicalException("SCALAR, '[', or '{' is expected, got ${scanner.tokens[0]}")
                }
            }
        }
    }

    // sequence: SEQUENCE-START (node (ENTRY node)*)? ENTRY? SEQUENCE-END
    private fun parseSequence() {
        scanner.getToken(Token.ID.FlowSequenceStart)
        if (!scanner.checkToken(Token.ID.FlowSequenceEnd)) {
            parseNode()
            while (!scanner.checkToken(Token.ID.FlowSequenceEnd)) {
                scanner.getToken(Token.ID.FlowEntry)
                if (!scanner.checkToken(Token.ID.FlowSequenceEnd)) {
                    parseNode()
                }
            }
        }
        scanner.getToken(Token.ID.FlowSequenceEnd)
        events.add(SequenceEndEvent(null, null))
    }

    // mapping: MAPPING-START (map_entry (ENTRY map_entry)*)? ENTRY? MAPPING-END
    private fun parseMapping() {
        scanner.getToken(Token.ID.FlowMappingStart)
        if (!scanner.checkToken(Token.ID.FlowMappingEnd)) {
            parseMapEntry()
            while (!scanner.checkToken(Token.ID.FlowMappingEnd)) {
                scanner.getToken(Token.ID.FlowEntry)
                if (!scanner.checkToken(Token.ID.FlowMappingEnd)) {
                    parseMapEntry()
                }
            }
        }
        scanner.getToken(Token.ID.FlowMappingEnd)
        events.add(MappingEndEvent(null, null))
    }

    // map_entry: KEY node VALUE node
    private fun parseMapEntry() {
        scanner.getToken(Token.ID.Key)
        parseNode()
        scanner.getToken(Token.ID.Value)
        parseNode()
    }

    private fun parse() {
        parseStream()
        parsed = true
    }

    override fun next(): Event {
        if (!parsed) {
            parse()
        }
        return events.removeAt(0) ?: throw NoSuchElementException("No more Events found.")
    }

    /**
     * Check the type of the next event.
     */
    override fun checkEvent(choice: Event.ID): Boolean {
        if (!parsed) {
            parse()
        }
        return if (events.isNotEmpty()) {
            events[0]?.eventId == choice
        } else {
            false
        }
    }

    /**
     * Get the next event.
     */
    override fun peekEvent(): Event {
        return peekEventInt() ?: throw NoSuchElementException("No more Events found.")
    }

    private fun peekEventInt(): Event? {
        if (!parsed) {
            parse()
        }
        return if (events.isEmpty()) {
            null
        } else {
            events[0]
        }
    }

    override fun hasNext(): Boolean {
        return peekEventInt() != null
    }

}
