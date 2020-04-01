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

import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.common.ArrayStack
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
import uk.org.jurg.yakl.engine.v2.exceptions.Mark
import uk.org.jurg.yakl.engine.v2.exceptions.ParserException
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.scanner.Scanner
import uk.org.jurg.yakl.engine.v2.scanner.ScannerImpl
import uk.org.jurg.yakl.engine.v2.scanner.StreamReader
import uk.org.jurg.yakl.engine.v2.tokens.AliasToken
import uk.org.jurg.yakl.engine.v2.tokens.AnchorToken
import uk.org.jurg.yakl.engine.v2.tokens.BlockEntryToken
import uk.org.jurg.yakl.engine.v2.tokens.DirectiveToken
import uk.org.jurg.yakl.engine.v2.tokens.ScalarToken
import uk.org.jurg.yakl.engine.v2.tokens.StreamEndToken
import uk.org.jurg.yakl.engine.v2.tokens.StreamStartToken
import uk.org.jurg.yakl.engine.v2.tokens.TAG_DIRECTIVE
import uk.org.jurg.yakl.engine.v2.tokens.TagToken
import uk.org.jurg.yakl.engine.v2.tokens.TagTuple
import uk.org.jurg.yakl.engine.v2.tokens.Token
import uk.org.jurg.yakl.engine.v2.tokens.Token.ID
import uk.org.jurg.yakl.engine.v2.tokens.YAML_DIRECTIVE

class ParserImpl(
    private val scanner: Scanner,
    private val settings: LoadSettings
) : Parser {

    constructor(reader: StreamReader, settings: LoadSettings) : this(ScannerImpl(reader), settings)

    private val defaultTags: Map<String, String> = mapOf(
        "!" to "!",
        "!!" to Tag.PREFIX
    )

    private var currentEvent: Event? = null
    private var directives = VersionTagsTuple(null, defaultTags)
    private val states = ArrayStack<Production>(100)
    private val marksStack = ArrayStack<Mark?>(10)
    private var state: Production? = ParseStreamStart()

    /**
     * Check the type of the next event.
     */
    override fun checkEvent(choice: Event.ID): Boolean {
        peekEvent()
        return currentEvent != null && currentEvent?.eventId == choice
    }

    private fun produce() {
        if (currentEvent == null) {
            currentEvent = state?.produce()
        }
    }

    /**
     * Get the next event.
     */
    override fun peekEvent(): Event {
        produce()
        return currentEvent ?: throw NoSuchElementException("No more Events found.")
    }

    /**
     * Get the next event and proceed further.
     */
    override fun next(): Event {
        peekEvent()
        val value = currentEvent ?: throw NoSuchElementException("No more Events found.")
        currentEvent = null
        return value
    }

    override fun hasNext(): Boolean {
        produce()
        return currentEvent != null
    }

    /**
     * <pre>
     * stream    ::= STREAM-START implicit_document? explicit_document* STREAM-END
     * implicit_document ::= block_node DOCUMENT-END*
     * explicit_document ::= DIRECTIVE* DOCUMENT-START block_node? DOCUMENT-END*
    </pre> *
     */
    private inner class ParseStreamStart : Production {
        override fun produce(): Event? { // Parse the stream start.
            val token = scanner.next() as StreamStartToken
            val event = StreamStartEvent(token.startMark, token.endMark)
            // Prepare the next state.
            state = ParseImplicitDocumentStart()
            return event
        }
    }

    private inner class ParseImplicitDocumentStart : Production {
        override fun produce(): Event? { // Parse an implicit document.
            return if (!scanner.checkToken(ID.Directive, ID.DocumentStart, ID.StreamEnd)) {
                directives = VersionTagsTuple(null, defaultTags)
                val token = scanner.peekToken()

                val event = DocumentStartEvent(false, null, emptyMap(), token.startMark, token.startMark)
                // Prepare the next state.
                states.push(ParseDocumentEnd())
                state = ParseBlockNode()
                event
            } else {
                ParseDocumentStart().produce()
            }
        }
    }

    private inner class ParseDocumentStart : Production {
        override fun produce(): Event? {
            // Parse any extra document end indicators.
            while (scanner.checkToken(ID.DocumentEnd)) {
                scanner.next()
            }
            // Parse an explicit document.
            val event: Event
            if (!scanner.checkToken(ID.StreamEnd)) {
                var token = scanner.peekToken()
                val startMark = token.startMark
                val tuple: VersionTagsTuple = processDirectives()
                if (!scanner.checkToken(ID.DocumentStart)) {
                    throw ParserException(
                        problem = "expected '<document start>', but found '${token.tokenId}'",
                        problemMark = token.startMark
                    )
                }
                token = scanner.next()

                event = DocumentStartEvent(true, tuple.specVersion, tuple.tags, startMark, token.endMark)
                states.push(ParseDocumentEnd())
                state = ParseDocumentContent()
            } else { // Parse the end of the stream.
                val token = scanner.next() as StreamEndToken
                event = StreamEndEvent(token.startMark, token.endMark)
                if (!states.isEmpty) {
                    throw YamlEngineException("Unexpected end of stream. States left: $states")
                }
                if (!markEmpty()) {
                    throw YamlEngineException("Unexpected end of stream. Marks left: $marksStack")
                }
                state = null
            }
            return event
        }

        private fun markEmpty(): Boolean {
            return marksStack.isEmpty
        }
    }

    private inner class ParseDocumentEnd : Production {
        override fun produce(): Event? {
            // Parse the document end.
            var token: Token = scanner.peekToken()
            val startMark = token.startMark
            var endMark = startMark
            var explicit = false
            if (scanner.checkToken(ID.DocumentEnd)) {
                token = scanner.next()
                endMark = token.endMark
                explicit = true
            }
            val event: Event = DocumentEndEvent(explicit, startMark, endMark)
            // Prepare the next state.
            state = ParseDocumentStart()
            return event
        }
    }

    private inner class ParseDocumentContent : Production {
        override fun produce(): Event? {
            val event: Event?
            return if (scanner.checkToken(ID.Directive, ID.DocumentStart, ID.DocumentEnd, ID.StreamEnd)) {
                event = processEmptyScalar(scanner.peekToken().startMark)
                state = states.pop()
                event
            } else {
                ParseBlockNode().produce()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processDirectives(): VersionTagsTuple {
        var yamlSpecVersion: SpecVersion? = null
        val tagHandles = HashMap<String, String>()
        while (scanner.checkToken(ID.Directive)) {
            val token = scanner.next() as DirectiveToken<*>
            token.value?.let { directiveValue ->

                //the value must be present
                when (token.name) {
                    YAML_DIRECTIVE -> {
                        if (yamlSpecVersion != null) {
                            throw ParserException(
                                problem = "found duplicate YAML directive",
                                problemMark = token.startMark
                            )
                        }
                        val value = directiveValue as List<Int>
                        val major = value[0]
                        val minor = value[1]
                        yamlSpecVersion = settings.versionFunction.invoke(SpecVersion(major, minor))
                    }
                    TAG_DIRECTIVE -> {
                        val value = directiveValue as List<String>
                        val handle = value[0]
                        val prefix = value[1]
                        if (tagHandles.containsKey(handle)) {
                            throw ParserException(
                                problem = "duplicate tag handle $handle",
                                problemMark = token.startMark
                            )
                        }
                        tagHandles[handle] = prefix
                    }
                }
            }
        }
        if (yamlSpecVersion == null || tagHandles.isNotEmpty()) {
            // directives in the document found - drop the previous
            for (entry in defaultTags.entries) {
                // do not overwrite re-defined tags
                if (!tagHandles.containsKey(entry.key)) {
                    tagHandles[entry.key] = entry.value
                }
            }
            directives = VersionTagsTuple(yamlSpecVersion, tagHandles)
        }
        return directives
    }

    /**
     * <pre>
     * block_node_or_indentless_sequence ::= ALIAS
     * | properties (block_content | indentless_block_sequence)?
     * | block_content
     * | indentless_block_sequence
     * block_node    ::= ALIAS
     * | properties block_content?
     * | block_content
     * flow_node     ::= ALIAS
     * | properties flow_content?
     * | flow_content
     * properties    ::= TAG ANCHOR? | ANCHOR TAG?
     * block_content     ::= block_collection | flow_collection | SCALAR
     * flow_content      ::= flow_collection | SCALAR
     * block_collection  ::= block_sequence | block_mapping
     * flow_collection   ::= flow_sequence | flow_mapping
    </pre> *
     */
    private inner class ParseBlockNode : Production {
        override fun produce(): Event? {
            return parseNode(block = true, indentlessSequence = false)
        }
    }

    private fun parseFlowNode(): Event? {
        return parseNode(block = false, indentlessSequence = false)
    }

    private fun parseBlockNodeOrIndentlessSequence(): Event? {
        return parseNode(block = true, indentlessSequence = true)
    }

    private fun parseNode(block: Boolean, indentlessSequence: Boolean): Event? {
        val event: Event
        var startMark: Mark? = null
        var endMark: Mark? = null
        var tagMark: Mark? = null
        if (scanner.checkToken(ID.Alias)) {
            val token = scanner.next() as AliasToken
            event = AliasEvent(token.value, token.startMark, token.endMark)
            state = states.pop()
        } else {
            var anchor: Anchor? = null
            var tagTupleValue: TagTuple? = null// Empty scalars are allowed even if a tag or an anchor is specified.
            when {
                scanner.checkToken(ID.Anchor) -> {
                    val token = scanner.next() as AnchorToken
                    startMark = token.startMark
                    endMark = token.endMark
                    anchor = token.value
                    if (scanner.checkToken(ID.Tag)) {
                        val tagToken = scanner.next() as TagToken
                        tagMark = tagToken.startMark
                        endMark = tagToken.endMark
                        tagTupleValue = tagToken.value
                    }
                }
                scanner.checkToken(ID.Tag) -> {
                    val tagToken = scanner.next() as TagToken
                    startMark = tagToken.startMark
                    tagMark = startMark
                    endMark = tagToken.endMark
                    tagTupleValue = tagToken.value
                    if (scanner.checkToken(ID.Anchor)) {
                        val token = scanner.next() as AnchorToken
                        endMark = token.endMark
                        anchor = token.value
                    }
                }
            }
            var tag: String? = null
            if (tagTupleValue != null) {
                val handle = tagTupleValue.handle
                val suffix = tagTupleValue.suffix
                tag = if (handle != null) {
                    if (!directives.tags.containsKey(handle)) {
                        throw ParserException(
                            context = "while parsing a node", contextMark = startMark,
                            problem = "found undefined tag handle $handle", problemMark = tagMark
                        )
                    }
                    directives.tags[handle].toString() + suffix
                } else {
                    suffix
                }
            }
            if (startMark == null) {
                startMark = scanner.peekToken().startMark
                endMark = startMark
            }
            val implicit = tag == null /* TODO issue 459 || tag.equals("!") */
            if (indentlessSequence && scanner.checkToken(ID.BlockEntry)) {
                endMark = scanner.peekToken().endMark
                event = SequenceStartEvent(anchor, tag, implicit, FlowStyle.BLOCK, startMark, endMark)
                state = ParseIndentlessSequenceEntry()
            } else {
                when {
                    scanner.checkToken(ID.Scalar) -> {
                        val token = scanner.next() as ScalarToken
                        endMark = token.endMark
                        val implicitValues = if (token.plain && tag == null /* TODO issue 459 || "!".equals(tag)*/) {
                            ImplicitTuple(plain = true, nonPlain = false)
                        } else if (tag == null) {
                            ImplicitTuple(plain = false, nonPlain = true)
                        } else {
                            ImplicitTuple(plain = false, nonPlain = false)
                        }
                        event = ScalarEvent(
                            anchor, tag, implicitValues, token.value, token.style,
                            startMark, endMark
                        )
                        state = states.pop()
                    }
                    scanner.checkToken(ID.FlowSequenceStart) -> {
                        endMark = scanner.peekToken().endMark
                        event = SequenceStartEvent(anchor, tag, implicit, FlowStyle.FLOW, startMark, endMark)
                        state = ParseFlowSequenceFirstEntry()
                    }
                    scanner.checkToken(ID.FlowMappingStart) -> {
                        endMark = scanner.peekToken().endMark
                        event = MappingStartEvent(
                            anchor, tag, implicit,
                            FlowStyle.FLOW, startMark, endMark
                        )
                        state = ParseFlowMappingFirstKey()
                    }
                    block && scanner.checkToken(ID.BlockSequenceStart) -> {
                        endMark = scanner.peekToken().startMark
                        event = SequenceStartEvent(anchor, tag, implicit, FlowStyle.BLOCK, startMark, endMark)
                        state = ParseBlockSequenceFirstEntry()
                    }
                    block && scanner.checkToken(ID.BlockMappingStart) -> {
                        endMark = scanner.peekToken().startMark
                        event = MappingStartEvent(
                            anchor, tag, implicit,
                            FlowStyle.BLOCK, startMark, endMark
                        )
                        state = ParseBlockMappingFirstKey()
                    }
                    anchor != null || tag != null -> {
                        // Empty scalars are allowed even if a tag or an anchor is specified.
                        event = ScalarEvent(
                            anchor, tag, ImplicitTuple(implicit, false), "", ScalarStyle.PLAIN,
                            startMark, endMark
                        )
                        state = states.pop()
                    }
                    else -> {
                        val node = if (block) {
                            "block"
                        } else {
                            "flow"
                        }
                        val token = scanner.peekToken()
                        throw ParserException(
                            context = "while parsing a $node node",
                            contextMark = startMark,
                            problem = "expected the node content, but found '${token.tokenId}'",
                            problemMark = token.startMark
                        )
                    }
                }
            }
        }
        return event
    }

    // block_sequence ::= BLOCK-SEQUENCE-START (BLOCK-ENTRY block_node?)*
    // BLOCK-END

    private inner class ParseBlockSequenceFirstEntry : Production {
        override fun produce(): Event? {
            val token: Token = scanner.next()
            markPush(token.startMark)
            return ParseBlockSequenceEntry().produce()
        }
    }

    private inner class ParseBlockSequenceEntry : Production {
        override fun produce(): Event? {
            if (scanner.checkToken(ID.BlockEntry)) {
                val token = scanner.next() as BlockEntryToken
                return if (!scanner.checkToken(
                        ID.BlockEntry,
                        ID.BlockEnd
                    )
                ) {
                    states.push(ParseBlockSequenceEntry())
                    ParseBlockNode().produce()
                } else {
                    state = ParseBlockSequenceEntry()
                    processEmptyScalar(token.endMark)
                }
            }
            if (!scanner.checkToken(ID.BlockEnd)) {
                val token: Token = scanner.peekToken()
                throw ParserException(
                    "while parsing a block collection", markPop(),
                    "expected <block end>, but found '" + token.tokenId + "'",
                    token.startMark
                )
            }
            val token: Token = scanner.next()
            val event: Event = SequenceEndEvent(token.startMark, token.endMark)
            state = states.pop()
            markPop()
            return event
        }
    }

    // indentless_sequence ::= (BLOCK-ENTRY block_node?)+

    // indentless_sequence ::= (BLOCK-ENTRY block_node?)+
    private inner class ParseIndentlessSequenceEntry : Production {
        override fun produce(): Event? {
            if (scanner.checkToken(ID.BlockEntry)) {
                val token: Token = scanner.next()
                return if (!scanner.checkToken(ID.BlockEntry, ID.Key, ID.Value, ID.BlockEnd)) {
                    states.push(ParseIndentlessSequenceEntry())
                    ParseBlockNode().produce()
                } else {
                    state = ParseIndentlessSequenceEntry()
                    processEmptyScalar(token.endMark)
                }
            }
            val token = scanner.peekToken()
            val event = SequenceEndEvent(token.startMark, token.endMark)
            state = states.pop()
            return event
        }
    }

    private inner class ParseBlockMappingFirstKey : Production {
        override fun produce(): Event? {
            val token: Token = scanner.next()
            markPush(token.startMark)
            return ParseBlockMappingKey().produce()
        }
    }

    private inner class ParseBlockMappingKey : Production {
        override fun produce(): Event? {
            if (scanner.checkToken(ID.Key)) {
                val token: Token = scanner.next()
                return if (!scanner.checkToken(ID.Key, ID.Value, ID.BlockEnd)) {
                    states.push(ParseBlockMappingValue())
                    parseBlockNodeOrIndentlessSequence()
                } else {
                    state = ParseBlockMappingValue()
                    processEmptyScalar(token.endMark)
                }
            }
            if (!scanner.checkToken(ID.BlockEnd)) {
                val token: Token = scanner.peekToken()
                throw ParserException(
                    "while parsing a block mapping", markPop(),
                    "expected <block end>, but found '" + token.tokenId + "'",
                    token.startMark
                )
            }
            val token = scanner.next()
            val event = MappingEndEvent(token.startMark, token.endMark)
            state = states.pop()
            markPop()
            return event
        }
    }

    private inner class ParseBlockMappingValue : Production {
        override fun produce(): Event? {
            if (scanner.checkToken(ID.Value)) {
                val token: Token = scanner.next()
                return if (!scanner.checkToken(
                        ID.Key,
                        ID.Value,
                        ID.BlockEnd
                    )
                ) {
                    states.push(ParseBlockMappingKey())
                    parseBlockNodeOrIndentlessSequence()
                } else {
                    state = ParseBlockMappingKey()
                    processEmptyScalar(token.endMark)
                }
            }
            state = ParseBlockMappingKey()
            return processEmptyScalar(scanner.peekToken().startMark)
        }
    }

    /**
     * <pre>
     * flow_sequence     ::= FLOW-SEQUENCE-START
     * (flow_sequence_entry FLOW-ENTRY)*
     * flow_sequence_entry?
     * FLOW-SEQUENCE-END
     * flow_sequence_entry   ::= flow_node | KEY flow_node? (VALUE flow_node?)?
     * Note that while production rules for both flow_sequence_entry and
     * flow_mapping_entry are equal, their interpretations are different.
     * For `flow_sequence_entry`, the part `KEY flow_node? (VALUE flow_node?)?`
     * generate an inline mapping (set syntax).
    </pre> *
     */
    private inner class ParseFlowSequenceFirstEntry : Production {
        override fun produce(): Event? {
            markPush(scanner.next().startMark)
            return ParseFlowSequenceEntry(true).produce()
        }
    }

    private inner class ParseFlowSequenceEntry(private val first: Boolean) : Production {
        override fun produce(): Event? {
            if (!scanner.checkToken(ID.FlowSequenceEnd)) {
                if (!first) {
                    if (scanner.checkToken(ID.FlowEntry)) {
                        scanner.next()
                    } else {
                        val token: Token = scanner.peekToken()
                        throw ParserException(
                            "while parsing a flow sequence", markPop(),
                            "expected ',' or ']', but got ${token.tokenId}",
                            token.startMark
                        )
                    }
                }
                if (scanner.checkToken(ID.Key)) {
                    val token: Token = scanner.peekToken()
                    val event: Event = MappingStartEvent(
                        null, null, true, FlowStyle.FLOW, token.startMark,
                        token.endMark
                    )
                    state = ParseFlowSequenceEntryMappingKey()
                    return event
                } else if (!scanner.checkToken(ID.FlowSequenceEnd)) {
                    states.push(ParseFlowSequenceEntry(false))
                    return parseFlowNode()
                }
            }
            val token = scanner.next()
            val event = SequenceEndEvent(token.startMark, token.endMark)
            state = states.pop()
            markPop()
            return event
        }
    }

    private inner class ParseFlowSequenceEntryMappingKey : Production {
        override fun produce(): Event? {
            val token: Token = scanner.next()
            return if (!scanner.checkToken(ID.Value, ID.FlowEntry, ID.FlowSequenceEnd)) {
                states.push(ParseFlowSequenceEntryMappingValue())
                parseFlowNode()
            } else {
                state = ParseFlowSequenceEntryMappingValue()
                processEmptyScalar(token.endMark)
            }
        }
    }

    private inner class ParseFlowSequenceEntryMappingValue : Production {
        override fun produce(): Event? {
            return if (scanner.checkToken(ID.Value)) {
                val token: Token = scanner.next()
                if (!scanner.checkToken(
                        ID.FlowEntry,
                        ID.FlowSequenceEnd
                    )
                ) {
                    states.push(ParseFlowSequenceEntryMappingEnd())
                    parseFlowNode()
                } else {
                    state = ParseFlowSequenceEntryMappingEnd()
                    processEmptyScalar(token.endMark)
                }
            } else {
                state = ParseFlowSequenceEntryMappingEnd()
                val token: Token = scanner.peekToken()
                processEmptyScalar(token.startMark)
            }
        }
    }

    private inner class ParseFlowSequenceEntryMappingEnd : Production {
        override fun produce(): Event? {
            state = ParseFlowSequenceEntry(false)
            val token = scanner.peekToken()
            return MappingEndEvent(token.startMark, token.endMark)
        }
    }

    /**
     * <pre>
     * flow_mapping  ::= FLOW-MAPPING-START
     * (flow_mapping_entry FLOW-ENTRY)*
     * flow_mapping_entry?
     * FLOW-MAPPING-END
     * flow_mapping_entry    ::= flow_node | KEY flow_node? (VALUE flow_node?)?
    </pre> *
     */
    private inner class ParseFlowMappingFirstKey : Production {
        override fun produce(): Event? {
            val token: Token = scanner.next()
            markPush(token.startMark)
            return ParseFlowMappingKey(true).produce()
        }
    }

    private inner class ParseFlowMappingKey(private val first: Boolean) : Production {
        override fun produce(): Event? {
            if (!scanner.checkToken(ID.FlowMappingEnd)) {
                if (!first) {
                    if (scanner.checkToken(ID.FlowEntry)) {
                        scanner.next()
                    } else {
                        val token: Token = scanner.peekToken()
                        throw ParserException(
                            "while parsing a flow mapping", markPop(),
                            "expected ',' or '}', but got ${token.tokenId}",
                            token.startMark
                        )
                    }
                }
                if (scanner.checkToken(ID.Key)) {
                    val token: Token = scanner.next()
                    return if (!scanner.checkToken(ID.Value, ID.FlowEntry, ID.FlowMappingEnd)) {
                        states.push(ParseFlowMappingValue())
                        parseFlowNode()
                    } else {
                        state = ParseFlowMappingValue()
                        processEmptyScalar(token.endMark)
                    }
                } else if (!scanner.checkToken(ID.FlowMappingEnd)) {
                    states.push(ParseFlowMappingEmptyValue())
                    return parseFlowNode()
                }
            }
            val token = scanner.next()
            val event = MappingEndEvent(token.startMark, token.endMark)
            state = states.pop()
            markPop()
            return event
        }
    }

    private inner class ParseFlowMappingValue : Production {
        override fun produce(): Event? {
            return if (scanner.checkToken(ID.Value)) {
                val token = scanner.next()
                if (!scanner.checkToken(ID.FlowEntry, ID.FlowMappingEnd)) {
                    states.push(ParseFlowMappingKey(false))
                    parseFlowNode()
                } else {
                    state = ParseFlowMappingKey(false)
                    processEmptyScalar(token.endMark)
                }
            } else {
                state = ParseFlowMappingKey(false)
                val token = scanner.peekToken()
                processEmptyScalar(token.startMark)
            }
        }
    }

    private inner class ParseFlowMappingEmptyValue : Production {
        override fun produce(): Event? {
            state = ParseFlowMappingKey(false)
            return processEmptyScalar(scanner.peekToken().startMark)
        }
    }

    /**
     * <pre>
     * block_mapping     ::= BLOCK-MAPPING_START
     * ((KEY block_node_or_indentless_sequence?)?
     * (VALUE block_node_or_indentless_sequence?)?)*
     * BLOCK-END
    </pre> *
     */
    private fun processEmptyScalar(mark: Mark?): Event? {
        return ScalarEvent(
            anchor = null,
            tag = null,
            implicit = ImplicitTuple(plain = true, nonPlain = false),
            value = "",
            scalarStyle = ScalarStyle.PLAIN,
            startMark = mark,
            endMark = mark
        )
    }

    private fun markPop(): Mark? {
        return marksStack.pop()
    }

    private fun markPush(mark: Mark?) {
        marksStack.push(mark)
    }
}
