package uk.org.jurg.yakl.engine.v2.emitter

import uk.org.jurg.yakl.engine.utils.Queue
import uk.org.jurg.yakl.engine.utils.Writer
import uk.org.jurg.yakl.engine.utils.charCount
import uk.org.jurg.yakl.engine.utils.codePointAt
import uk.org.jurg.yakl.engine.utils.isPrintable
import uk.org.jurg.yakl.engine.utils.toCodePoint
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.common.ArrayStack
import uk.org.jurg.yakl.engine.v2.common.CharConstants
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.common.SpecVersion
import uk.org.jurg.yakl.engine.v2.events.AliasEvent
import uk.org.jurg.yakl.engine.v2.events.CollectionEndEvent
import uk.org.jurg.yakl.engine.v2.events.CollectionStartEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentEndEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentStartEvent
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.events.MappingStartEvent
import uk.org.jurg.yakl.engine.v2.events.NodeEvent
import uk.org.jurg.yakl.engine.v2.events.ScalarEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceStartEvent
import uk.org.jurg.yakl.engine.v2.exceptions.EmitterException
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.Tag

const val MIN_INDENT = 1
const val MAX_INDENT = 10
private const val SPACE = " "

class Emitter(private val opts: DumpSettings, private val stream: Writer) : Emitable {

    private val ESCAPE_REPLACEMENTS: Map<Char, String> = mapOf(
        '\u0000' to "0",
        '\u0007' to "a",
        '\u0008' to "b",
        '\u0009' to "t",
        '\n'     to "n",
        '\u000B' to "v",
        '\u000C' to "f",
        '\r'     to "r",
        '\u001B' to "e",
        '"'      to "\"",
        '\\'     to "\\",
        '\u0085' to "N",
        '\u00A0' to "_",
        '\u2028' to "L",
        '\u2029' to "P"
    )

    private val DEFAULT_TAG_PREFIXES: Map<String, String> = linkedMapOf(
        "!" to  "!",
        Tag.PREFIX to "!!"
    )

    // Emitter is a state machine with a stack of states to handle nested structures.
    private val states = ArrayStack<EmitterState>(100)
    private var state: EmitterState = ExpectStreamStart()

    // Current event and the event queue.
    private val events: Queue<Event> = Queue(100)
    private var event: Event? = null

    // The current indentation level and the stack of previous indents.
    private val indents = ArrayStack<Int?>(10)

    private var indent: Int? = null

    // Flow level.
    private var flowLevel = 0

    // Contexts.
    private var rootContext = false
    private var mappingContext = false
    private var simpleKeyContext = false

    //
    // Characteristics of the last emitted character:
    // - current position.
    // - is it a whitespace?
    // - is it an indention character (indentation space, '-', '?', or ':')?
    private var column = 0
    private var whitespace = true
    private var indention = true
    private var openEnded = false

    private val bestIndent = if (opts.indent in (MIN_INDENT + 1) until MAX_INDENT) opts.indent else 2
    private val bestWidth = if (opts.width > bestIndent * 2) opts.width else 80

    // Tag prefixes.
    private var tagPrefixes = LinkedHashMap<String, String>()

    // Prepared anchor and tag.
    private var preparedAnchor: Anchor? = null
    private var preparedTag: String? = null

    // Scalar analysis and style.
    private var analysis: ScalarAnalysis? = null
    private var scalarStyle: ScalarStyle? = null

    override fun emit(event: Event) {
        events.add(event)
        while (!needMoreEvents()) {
            this.event = events.poll()
            state.expect()
            this.event = null
        }
    }

    // In some cases, we wait for a few next events before emitting.
    private fun needMoreEvents(): Boolean {
        if (events.isEmpty()) {
            return true
        }
        val nextEvent = events.peek()
        return when (nextEvent.eventId) {
            Event.ID.DocumentStart -> needEvents(1)
            Event.ID.SequenceStart -> needEvents(2)
            Event.ID.MappingStart -> needEvents(3)
            else -> false
        }
    }

    private fun needEvents(count: Int): Boolean {
        var level = 0
        events.drop(1).forEach {
            when {
                it.eventId === Event.ID.DocumentStart || it is CollectionStartEvent -> level++
                it.eventId === Event.ID.DocumentEnd || it is CollectionEndEvent -> level--
                it.eventId === Event.ID.StreamEnd -> level = -1
            }
            if (level < 0) {
                return false
            }
        }
        return events.size < count + 1
    }

    private fun increaseIndent(isFlow: Boolean, indentless: Boolean) {
        indents.push(indent)

        if (indent == null) {
            indent = if (isFlow) {
                bestIndent
            } else {
                0
            }
        } else if (!indentless) {
            indent = indent!! + bestIndent
        }
    }

    // States
    // Stream handlers.
    private inner class ExpectStreamStart : EmitterState {
        override fun expect() {
            if (event?.eventId == Event.ID.StreamStart) {
                writeStreamStart()
                state = ExpectFirstDocumentStart()
            } else {
                throw EmitterException("expected StreamStartEvent, but got $event")
            }
        }
    }

    private inner class ExpectNothing : EmitterState {
        override fun expect() {
            throw EmitterException("expecting nothing, but got $event")
        }
    }

    // Document handlers.
    private inner class ExpectFirstDocumentStart : EmitterState {
        override fun expect() {
            ExpectDocumentStart(true).expect()
        }
    }

    private inner class ExpectDocumentStart(private val first: Boolean) : EmitterState {
        override fun expect() {
            state = when (event?.eventId) {
                Event.ID.DocumentStart -> {
                    val ev = event as DocumentStartEvent
                    handleDocumentStartEvent(ev)
                    ExpectDocumentRoot()
                }
                Event.ID.StreamEnd -> {
                    writeStreamEnd()
                    ExpectNothing()
                }
                else -> {
                    throw EmitterException("expected DocumentStartEvent, but got $event")
                }
            }
        }

        private fun handleDocumentStartEvent(ev: DocumentStartEvent) {
            if ((ev.specVersion != null || ev.tags.isNotEmpty()) && openEnded) {
                writeIndicator("...", needWhitespace = true, whitespace = false, indentation = false)
                writeIndent()
            }
            if (ev.specVersion != null) {
                writeVersionDirective(prepareVersion(ev.specVersion))
            }
            tagPrefixes = LinkedHashMap(DEFAULT_TAG_PREFIXES)
            if (ev.tags.isNotEmpty()) {
                handleTagDirectives(ev.tags)
            }
            val implicit = (first && !ev.explicit && !opts.canonical
                    && ev.specVersion == null
                    && ev.tags.isEmpty()
                    && !checkEmptyDocument())
            if (!implicit) {
                writeIndent()
                writeIndicator("---", needWhitespace = true, whitespace = false, indentation = false)
                if (opts.canonical) {
                    writeIndent()
                }
            }
        }

        private fun handleTagDirectives(tags: Map<String, String>) {
            tags.keys.asSequence().sorted().forEach {
                val prefix = tags.getValue(it)
                tagPrefixes[prefix] = it
                val handleText: String = prepareTagHandle(it)
                val prefixText: String = prepareTagPrefix(prefix)
                writeTagDirective(handleText, prefixText)
            }
        }

        private fun checkEmptyDocument(): Boolean {
            if (event?.eventId != Event.ID.DocumentStart || events.isEmpty()) {
                return false
            }
            val nextEvent: Event = events.peek()
            if (nextEvent.eventId == Event.ID.Scalar) {
                val e = nextEvent as ScalarEvent
                return e.anchor == null && e.tag == null && e.value.isEmpty()
            }
            return false
        }
    }

    private inner class ExpectDocumentEnd : EmitterState {
        override fun expect() {
            if (event?.eventId == Event.ID.DocumentEnd) {
                writeIndent()
                if ((event as DocumentEndEvent).explicit) {
                    writeIndicator("...", needWhitespace = true, whitespace = false, indentation = false)
                    writeIndent()
                }
                flushStream()
                state = ExpectDocumentStart(false)
            } else {
                throw EmitterException("expected DocumentEndEvent, but got $event")
            }
        }
    }

    private inner class ExpectDocumentRoot : EmitterState {
        override fun expect() {
            states.push(ExpectDocumentEnd())
            expectNode(root = true, mapping = false, simpleKey = false)
        }
    }

    // Node handlers.

    // Node handlers.
    private fun expectNode(root: Boolean, mapping: Boolean, simpleKey: Boolean) {
        rootContext = root
        mappingContext = mapping
        simpleKeyContext = simpleKey
        when (val eventId = this.event!!.eventId) {
            Event.ID.Alias -> {
                expectAlias()
            }
            Event.ID.Scalar, Event.ID.SequenceStart, Event.ID.MappingStart -> {
                processAnchor("&")
                processTag()
                handleNodeEvent(eventId)
            }
            else -> {
                throw EmitterException("expected NodeEvent, but got $event")
            }
        }
    }

    private fun handleNodeEvent(id: Event.ID) {
        when (id) {
            Event.ID.Scalar -> expectScalar()
            Event.ID.SequenceStart -> {
                if (flowLevel != 0 || opts.canonical || (event as SequenceStartEvent).isFlow() || checkEmptySequence()) {
                    expectFlowSequence()
                } else {
                    expectBlockSequence()
                }
            }
            Event.ID.MappingStart -> {
                if (flowLevel != 0 || opts.canonical || (event as MappingStartEvent).isFlow() || checkEmptyMapping()) {
                    expectFlowMapping()
                } else {
                    expectBlockMapping()
                }
            }
            else -> throw IllegalStateException()
        }
    }

    private fun expectAlias() {
        state = if (event is AliasEvent) {
            processAnchor("*")
            states.pop()
        } else {
            throw EmitterException("Expecting Alias.")
        }
    }

    private fun expectScalar() {
        increaseIndent(isFlow = true, indentless = false)
        processScalar()
        indent = indents.pop()
        state = states.pop()
    }

    // Flow sequence handlers.

    private fun expectFlowSequence() {
        writeIndicator("[", needWhitespace = true, whitespace = true, indentation = false)
        flowLevel++
        increaseIndent(isFlow = true, indentless = false)
        if (opts.multiLineFlow) {
            writeIndent()
        }
        state = ExpectFirstFlowSequenceItem()
    }

    private inner class ExpectFirstFlowSequenceItem : EmitterState {
        override fun expect() {
            if (event?.eventId == Event.ID.SequenceEnd) {
                indent = indents.pop()
                flowLevel--
                writeIndicator("]", needWhitespace = false, whitespace = false, indentation = false)
                state = states.pop()
            } else {
                if (opts.canonical || column > bestWidth && opts.splitLines || opts.multiLineFlow) {
                    writeIndent()
                }
                states.push(ExpectFlowSequenceItem())
                expectNode(root = false, mapping = false, simpleKey = false)
            }
        }
    }

    private inner class ExpectFlowSequenceItem : EmitterState {
        override fun expect() {
            if (event?.eventId == Event.ID.SequenceEnd) {
                indent = indents.pop()
                flowLevel--
                if (opts.canonical) {
                    writeIndicator(",", needWhitespace = false, whitespace = false, indentation = false)
                    writeIndent()
                }
                writeIndicator("]", needWhitespace = false, whitespace = false, indentation = false)
                if (opts.multiLineFlow) {
                    writeIndent()
                }
                state = states.pop()
            } else {
                writeIndicator(",", needWhitespace = false, whitespace = false, indentation = false)
                if (opts.canonical || column > bestWidth && opts.splitLines || opts.multiLineFlow) {
                    writeIndent()
                }
                states.push(ExpectFlowSequenceItem())
                expectNode(root = false, mapping = false, simpleKey = false)
            }
        }
    }

    // Flow mapping handlers.

    private fun expectFlowMapping() {
        writeIndicator("{", needWhitespace = true, whitespace = true, indentation = false)
        flowLevel++
        increaseIndent(isFlow = true, indentless = false)
        if (opts.multiLineFlow) {
            writeIndent()
        }
        state = ExpectFirstFlowMappingKey()
    }

    private inner class ExpectFirstFlowMappingKey : EmitterState {
        override fun expect() {
            if (event?.eventId == Event.ID.MappingEnd) {
                indent = indents.pop()
                flowLevel--
                writeIndicator("}", needWhitespace = false, whitespace = false, indentation = false)
                state = states.pop()
            } else {
                if (opts.canonical || column > bestWidth && opts.splitLines || opts.multiLineFlow) {
                    writeIndent()
                }
                if (!opts.canonical && checkSimpleKey()) {
                    states.push(ExpectFlowMappingSimpleValue())
                    expectNode(root = false, mapping = true, simpleKey = true)
                } else {
                    writeIndicator("?", needWhitespace = true, whitespace = false, indentation = false)
                    states.push(ExpectFlowMappingValue())
                    expectNode(root = false, mapping = true, simpleKey = false)
                }
            }
        }
    }

    private inner class ExpectFlowMappingKey : EmitterState {
        override fun expect() {
            if (event?.eventId == Event.ID.MappingEnd) {
                indent = indents.pop()
                flowLevel--
                if (opts.canonical) {
                    writeIndicator(",", needWhitespace = false, whitespace = false, indentation = false)
                    writeIndent()
                }
                if (opts.multiLineFlow) {
                    writeIndent()
                }
                writeIndicator("}", needWhitespace = false, whitespace = false, indentation = false)
                state = states.pop()
            } else {
                writeIndicator(",", needWhitespace = false, whitespace = false, indentation = false)
                if (opts.canonical || column > bestWidth && opts.splitLines || opts.multiLineFlow) {
                    writeIndent()
                }
                if (!opts.canonical && checkSimpleKey()) {
                    states.push(ExpectFlowMappingSimpleValue())
                    expectNode(root = false, mapping = true, simpleKey = true)
                } else {
                    writeIndicator("?", needWhitespace = true, whitespace = false, indentation = false)
                    states.push(ExpectFlowMappingValue())
                    expectNode(root = false, mapping = true, simpleKey = false)
                }
            }
        }
    }

    private inner class ExpectFlowMappingSimpleValue : EmitterState {
        override fun expect() {
            writeIndicator(":", needWhitespace = false, whitespace = false, indentation = false)
            states.push(ExpectFlowMappingKey())
            expectNode(root = false, mapping = true, simpleKey = false)
        }
    }

    private inner class ExpectFlowMappingValue : EmitterState {
        override fun expect() {
            if (opts.canonical || column > bestWidth || opts.multiLineFlow) {
                writeIndent()
            }
            writeIndicator(":", needWhitespace = true, whitespace = false, indentation = false)
            states.push(ExpectFlowMappingKey())
            expectNode(root = false, mapping = true, simpleKey = false)
        }
    }

    // Block sequence handlers.
    private fun expectBlockSequence() {
        val indentless = mappingContext && !indention
        increaseIndent(false, indentless)
        state = ExpectFirstBlockSequenceItem()
    }

    private inner class ExpectFirstBlockSequenceItem : EmitterState {
        override fun expect() {
            ExpectBlockSequenceItem(true).expect()
        }
    }

    private inner class ExpectBlockSequenceItem(private val first: Boolean) : EmitterState {
        override fun expect() {
            if (!first && event?.eventId == Event.ID.SequenceEnd) {
                indent = indents.pop()
                state = states.pop()
            } else {
                writeIndent()
                writeWhitespace(opts.indicatorIndent)
                writeIndicator("-", needWhitespace = true, whitespace = false, indentation = true)
                states.push(ExpectBlockSequenceItem(false))
                expectNode(root = false, mapping = false, simpleKey = false)
            }
        }
    }

    // Block mapping handlers.
    private fun expectBlockMapping() {
        increaseIndent(isFlow = false, indentless = false)
        state = ExpectFirstBlockMappingKey()
    }

    private inner class ExpectFirstBlockMappingKey : EmitterState {
        override fun expect() {
            ExpectBlockMappingKey(true).expect()
        }
    }

    private inner class ExpectBlockMappingKey(private val first: Boolean) : EmitterState {
        override fun expect() {
            if (!first && event?.eventId == Event.ID.MappingEnd) {
                indent = indents.pop()
                state = states.pop()
            } else {
                writeIndent()
                if (checkSimpleKey()) {
                    states.push(ExpectBlockMappingSimpleValue())
                    expectNode(root = false, mapping = true, simpleKey = true)
                } else {
                    writeIndicator("?", needWhitespace = true, whitespace = false, indentation = true)
                    states.push(ExpectBlockMappingValue())
                    expectNode(root = false, mapping = true, simpleKey = false)
                }
            }
        }
    }

    private inner class ExpectBlockMappingSimpleValue : EmitterState {
        override fun expect() {
            writeIndicator(":", needWhitespace = false, whitespace = false, indentation = false)
            states.push(ExpectBlockMappingKey(false))
            expectNode(root = false, mapping = true, simpleKey = false)
        }
    }

    private inner class ExpectBlockMappingValue : EmitterState {
        override fun expect() {
            writeIndent()
            writeIndicator(":", needWhitespace = true, whitespace = false, indentation = true)
            states.push(ExpectBlockMappingKey(false))
            expectNode(root = false, mapping = true, simpleKey = false)
        }
    }

    // Checkers.

    private fun checkEmptySequence(): Boolean {
        return event?.eventId == Event.ID.SequenceStart && events.isNotEmpty() && events.peek().eventId == Event.ID.SequenceEnd
    }

    private fun checkEmptyMapping(): Boolean {
        return event?.eventId == Event.ID.MappingStart && events.isNotEmpty() && events.peek().eventId == Event.ID.MappingEnd
    }

    private fun checkSimpleKey(): Boolean {
        var length = 0
        val event = this.event!!
        if (event is NodeEvent) {
            val anchorOpt = event.anchor
            if (anchorOpt != null) {
                if (preparedAnchor == null) {
                    preparedAnchor = anchorOpt
                }
                length += anchorOpt.value.length
            }
        }
        val tag = when {
            event.eventId == Event.ID.Scalar -> (event as ScalarEvent).tag
            event is CollectionStartEvent -> event.tag
            else -> null
        }
        if (tag != null) {
            if (preparedTag == null) {
                preparedTag = prepareTag(tag)
            }
            length += preparedTag!!.length
        }
        if (event.eventId == Event.ID.Scalar) {
            if (analysis == null) {
                analysis = analyzeScalar((event as ScalarEvent).value)
            }
            length += analysis!!.scalar.length
        }
        return length < opts.maxSimpleKeyLength && (
                event.eventId == Event.ID.Alias ||
                        event.eventId == Event.ID.Scalar &&
                        !this.analysis!!.empty &&
                        !this.analysis!!.multiline ||
                        checkEmptySequence() ||
                        checkEmptyMapping())
    }

    // Anchor, Tag, and Scalar processors.
    private fun processAnchor(indicator: String) {
        val anchor = (event as NodeEvent).anchor
        if (anchor != null) {
            if (preparedAnchor == null) {
                preparedAnchor = anchor
            }
            writeIndicator(indicator + anchor, needWhitespace = true, whitespace = false, indentation = false)
        }
        preparedAnchor = null
    }

    private fun processTag() {
        var tag: String?
        if (event!!.eventId == Event.ID.Scalar) {
            val ev = event as ScalarEvent
            tag = ev.tag
            if (scalarStyle == null) {
                scalarStyle = chooseScalarStyle(ev)
            }
            if ((!opts.canonical || tag == null) &&
                (scalarStyle == null && ev.implicit.canOmitTagInPlainScalar() ||
                        scalarStyle != null && ev.implicit.canOmitTagInNonPlainScalar())) {
                preparedTag = null
                return
            }
            if (ev.implicit.canOmitTagInPlainScalar() && tag == null) {
                tag = "!"
                preparedTag = null
            }
        } else {
            val ev = event as CollectionStartEvent
            tag = ev.tag
            if ((!opts.canonical || tag == null) && ev.implicit) {
                preparedTag = null
                return
            }
        }
        tag ?: throw EmitterException("tag is not specified")

        if (preparedTag == null) {
            preparedTag = prepareTag(tag)
        }
        writeIndicator(preparedTag!!, needWhitespace = true, whitespace = false, indentation = false)
        preparedTag = null
    }

    private fun chooseScalarStyle(ev: ScalarEvent): ScalarStyle? {
        val analysis = this.analysis ?: analyzeScalar(ev.value)
        this.analysis = analysis

        if (!ev.isPlain() && ev.scalarStyle == ScalarStyle.DOUBLE_QUOTED || opts.canonical) {
            return ScalarStyle.DOUBLE_QUOTED
        }
        if (ev.isPlain() && ev.implicit.canOmitTagInPlainScalar()) {
            if (!(simpleKeyContext && (analysis.empty || analysis.multiline))
                && (flowLevel != 0 && analysis.allowFlowPlain || flowLevel == 0 && analysis.allowBlockPlain)) {
                return null
            }
        }
        if (!ev.isPlain() && (ev.scalarStyle == ScalarStyle.LITERAL || ev.scalarStyle == ScalarStyle.FOLDED)) {
            if (flowLevel == 0 && !simpleKeyContext && analysis.allowBlock) {
                return ev.scalarStyle
            }
        }
        if (ev.isPlain() || ev.scalarStyle == ScalarStyle.SINGLE_QUOTED) {
            if (analysis.allowSingleQuoted && !(simpleKeyContext && analysis.multiline)) {
                return ScalarStyle.SINGLE_QUOTED
            }
        }
        return ScalarStyle.DOUBLE_QUOTED
    }

    private fun processScalar() {
        val ev = event as ScalarEvent

        val analysis = this.analysis ?: analyzeScalar(ev.value)
        this.analysis = analysis
        this.scalarStyle = this.scalarStyle ?: chooseScalarStyle(ev)

        val split = !simpleKeyContext && opts.splitLines
        if (scalarStyle == null) {
            writePlain(analysis.scalar, split)
        } else {
            when (scalarStyle) {
                ScalarStyle.DOUBLE_QUOTED -> writeDoubleQuoted(analysis.scalar, split)
                ScalarStyle.SINGLE_QUOTED -> writeSingleQuoted(analysis.scalar, split)
                ScalarStyle.FOLDED -> writeFolded(analysis.scalar, split)
                ScalarStyle.LITERAL -> writeLiteral(analysis.scalar)
                else -> throw YamlEngineException("Unexpected scalarStyle: $scalarStyle")
            }
        }
        this.analysis = null
        this.scalarStyle = null
    }

    // Analyzers.
    private fun prepareVersion(version: SpecVersion): String {
        if (version.major != 1) {
            throw EmitterException("unsupported YAML version: $version")
        }
        return version.representation
    }

    private val handleFormat = "^![-_\\w]*!$".toRegex()

    private fun prepareTagHandle(handle: String): String {
        if (handle.isEmpty()) {
            throw EmitterException("tag handle must not be empty")
        }
        if (handle.first() != '!' || handle.last() != '!') {
            throw EmitterException("tag handle must start and end with '!': $handle")
        }
        if ("!" != handle && !handleFormat.matches(handle)) {
            throw EmitterException("invalid character in the tag handle: $handle")
        }
        return handle
    }

    private fun prepareTagPrefix(prefix: String): String {
        if (prefix.isEmpty()) {
            throw EmitterException("tag prefix must not be empty")
        }
        // SnakeYAML seems to do something totally pointless here
        return prefix
    }

    private fun prepareTag(tag: String): String {
        if (tag.isEmpty()) {
            throw EmitterException("tag must not be empty")
        }
        if ("!" == tag) {
            return tag
        }

        // shall the tag prefixes be sorted as in PyYAML?
        var handle = tagPrefixes.keys
            .firstOrNull { tag.startsWith(it) && ("!" == it || it.length < tag.length) }

        var suffix = tag
        if (handle != null) {
            suffix = tag.substring(handle.length)
            handle = tagPrefixes[handle]
        }
        val end = suffix.length
        val suffixText = if (end > 0) suffix.substring(0, end) else ""
        return if (handle != null) handle + suffixText else "!<$suffixText>"
    }

    private fun analyzeScalar(scalar: String): ScalarAnalysis {
        // Empty scalar is a special case.
        if (scalar.isEmpty()) {
            return ScalarAnalysis(scalar,
                empty = true,
                multiline = false,
                allowFlowPlain = false,
                allowBlockPlain = true,
                allowSingleQuoted = true,
                allowBlock = false
            )
        }
        // Indicators and special characters.
        var blockIndicators = false
        var flowIndicators = false
        var lineBreaks = false
        var specialCharacters = false

        // Important whitespace combinations.
        var leadingSpace = false
        var leadingBreak = false
        var trailingSpace = false
        var trailingBreak = false
        var breakSpace = false
        var spaceBreak = false

        // Check document indicators.
        if (scalar.startsWith("---") || scalar.startsWith("...")) {
            blockIndicators = true
            flowIndicators = true
        }
        // First character or preceded by a whitespace.
        var preceededByWhitespace = true
        var followedByWhitespace =
            scalar.length == 1 || CharConstants.NULL_BL_T_LINEBR.has(scalar.codePointAt(1))
        // The previous character is a space.
        var previousSpace = false

        // The previous character is a break.
        var previousBreak = false
        var index = 0
        while (index < scalar.length) {
            val c: Int = scalar.codePointAt(index)
            // Check for indicators.
            if (index == 0) {
                // Leading indicators are special characters.
                if ("#,[]{}&*!|>\'\"%@`".contains(char = c.toChar())) {
                    flowIndicators = true
                    blockIndicators = true
                }
                if (c == '?'.toInt() || c == ':'.toInt()) {
                    flowIndicators = true
                    if (followedByWhitespace) {
                        blockIndicators = true
                    }
                }
                if (c == '-'.toInt() && followedByWhitespace) {
                    flowIndicators = true
                    blockIndicators = true
                }
            } else {
                // Some indicators cannot appear within a scalar as well.
                if (",?[]{}".contains(char = c.toChar())) {
                    flowIndicators = true
                }
                if (c == ':'.toInt()) {
                    flowIndicators = true
                    if (followedByWhitespace) {
                        blockIndicators = true
                    }
                }
                if (c == '#'.toInt() && preceededByWhitespace) {
                    flowIndicators = true
                    blockIndicators = true
                }
            }
            // Check for line breaks, special, and unicode characters.
            val isLineBreak = CharConstants.LINEBR.has(c)
            if (isLineBreak) {
                lineBreaks = true
            }
            if (!(c == '\n'.toInt() || c in 0x20..0x7E)) {
                if (c == 0x85 || c in 0xA0..0xD7FF
                    || c in 0xE000..0xFFFD
                    || c in 0x10000..0x10FFFF
                ) {
                    // unicode is used
                    if (!opts.useUnicodeEncoding) {
                        specialCharacters = true
                    }
                } else {
                    specialCharacters = true
                }
            }
            // Detect important whitespace combinations.
            when {
                c == ' '.toInt() -> {
                    if (index == 0) {
                        leadingSpace = true
                    }
                    if (index == scalar.length - 1) {
                        trailingSpace = true
                    }
                    if (previousBreak) {
                        breakSpace = true
                    }
                    previousSpace = true
                    previousBreak = false
                }
                isLineBreak -> {
                    if (index == 0) {
                        leadingBreak = true
                    }
                    if (index == scalar.length - 1) {
                        trailingBreak = true
                    }
                    if (previousSpace) {
                        spaceBreak = true
                    }
                    previousSpace = false
                    previousBreak = true
                }
                else -> {
                    previousSpace = false
                    previousBreak = false
                }
            }

            // Prepare for the next character.
            index += c.charCount()
            preceededByWhitespace = CharConstants.NULL_BL_T.has(c) || isLineBreak
            followedByWhitespace = true
            if (index + 1 < scalar.length) {
                val nextIndex = index + scalar.codePointAt(index).charCount()
                if (nextIndex < scalar.length) {
                    followedByWhitespace = CharConstants.NULL_BL_T.has(scalar.codePointAt(nextIndex)) || isLineBreak
                }
            }
        }
        // Let's decide what styles are allowed.
        var allowFlowPlain = true
        var allowBlockPlain = true
        var allowSingleQuoted = true
        var allowBlock = true
        // Leading and trailing whitespaces are bad for plain scalars.
        if (leadingSpace || leadingBreak || trailingSpace || trailingBreak) {
            allowBlockPlain = false
            allowFlowPlain = false
        }
        // We do not permit trailing spaces for block scalars.
        if (trailingSpace) {
            allowBlock = false
        }
        // Spaces at the beginning of a new line are only acceptable for block
        // scalars.
        if (breakSpace) {
            allowSingleQuoted = false
            allowBlockPlain = false
            allowFlowPlain = false
        }
        // Spaces followed by breaks, as well as special character are only
        // allowed for double quoted scalars.
        if (spaceBreak || specialCharacters) {
            allowBlock = false
            allowSingleQuoted = false
            allowBlockPlain = false
            allowFlowPlain = false
        }
        // Although the plain scalar writer supports breaks, we never emit
        // multiline plain scalars in the flow context.
        if (lineBreaks) {
            allowFlowPlain = false
        }
        // Flow indicators are forbidden for flow plain scalars.
        if (flowIndicators) {
            allowFlowPlain = false
        }
        // Block indicators are forbidden for block plain scalars.
        if (blockIndicators) {
            allowBlockPlain = false
        }
        return ScalarAnalysis(
            scalar = scalar,
            empty = false,
            multiline = lineBreaks,
            allowFlowPlain = allowFlowPlain,
            allowBlockPlain = allowBlockPlain,
            allowSingleQuoted = allowSingleQuoted,
            allowBlock = allowBlock
        )
    }

    // Writers.
    fun flushStream() {
        stream.flush()
    }

    fun writeStreamStart() {
        // BOM is written by Writer.
    }

    fun writeStreamEnd() {
        flushStream()
    }

    fun writeIndicator(
        indicator: String, needWhitespace: Boolean, whitespace: Boolean,
        indentation: Boolean
    ) {
        if (!this.whitespace && needWhitespace) {
            column++
            stream.write(SPACE)
        }
        this.whitespace = whitespace
        indention = indention && indentation
        column += indicator.length
        openEnded = false
        stream.write(indicator)
    }

    fun writeIndent() {
        val indentToWrite = indent ?: 0

        if (!indention || column > indentToWrite || column == indentToWrite && !whitespace) {
            writeLineBreak(null)
        }
        writeWhitespace(indentToWrite - column)
    }

    private fun writeWhitespace(length: Int) {
        if (length <= 0) {
            return
        }
        whitespace = true
        for (i in 0 until length) {
            stream.write(" ")
        }
        column += length
    }

    private fun writeLineBreak(data: String?) {
        whitespace = true
        indention = true
        column = 0
        if (data == null) {
            stream.write(opts.bestLineBreak)
        } else {
            stream.write(data)
        }
    }

    fun writeVersionDirective(versionText: String) {
        stream.write("%YAML ")
        stream.write(versionText)
        writeLineBreak(null)
    }

    fun writeTagDirective(handleText: String, prefixText: String) {
        // XXX: not sure 4 invocations better then StringBuilders created by str
        // + str
        stream.write("%TAG ")
        stream.write(handleText)
        stream.write(SPACE)
        stream.write(prefixText)
        writeLineBreak(null)
    }

    // Scalar streams.
    @OptIn(ExperimentalStdlibApi::class)
    private fun writeSingleQuoted(text: String, split: Boolean) {
        writeIndicator("'", needWhitespace = true, whitespace = false, indentation = false)
        var spaces = false
        var breaks = false
        var start = 0
        var end = 0
        var ch: Char
        while (end <= text.length) {
            ch = 0.toChar()
            if (end < text.length) {
                ch = text[end]
            }
            if (spaces) {
                if (ch.toInt() == 0 || ch != ' ') {
                    if (start + 1 == end && column > bestWidth && split && start != 0 && end != text.length) {
                        writeIndent()
                    } else {
                        val len = end - start
                        column += len
                        stream.write(text, start, len)
                    }
                    start = end
                }
            } else if (breaks) {
                if (ch.toInt() == 0 || CharConstants.LINEBR.hasNo(ch.toInt())) {
                    if (text[start] == '\n') {
                        writeLineBreak(null)
                    }
                    val data = text.substring(start, end)
                    for (br in data.toCharArray()) {
                        if (br == '\n') {
                            writeLineBreak(null)
                        } else {
                            writeLineBreak(br.toString())
                        }
                    }
                    writeIndent()
                    start = end
                }
            } else {
                if (CharConstants.LINEBR.has(ch.toInt(), "\u0000 \'")) {
                    if (start < end) {
                        val len = end - start
                        column += len
                        stream.write(text, start, len)
                        start = end
                    }
                }
            }
            if (ch == '\'') {
                column += 2
                stream.write("''")
                start = end + 1
            }
            if (ch.toInt() != 0) {
                spaces = ch == ' '
                breaks = CharConstants.LINEBR.has(ch.toInt())
            }
            end++
        }
        writeIndicator("'", needWhitespace = false, whitespace = false, indentation = false)
    }

    private fun writeDoubleQuoted(text: String, split: Boolean) {
        writeIndicator("\"", needWhitespace = true, whitespace = false, indentation = false)
        var start = 0
        var end = 0
        while (end <= text.length) {
            val ch = if (end < text.length) text[end] else null
            if (ch == null || "\"\\\u0085\u2028\u2029\uFEFF".contains(char = ch) || !('\u0020' <= ch && ch <= '\u007E')) {
                if (start < end) {
                    val len = end - start
                    column += len
                    stream.write(text, start, len)
                    start = end
                }
                if (ch != null) {
                    val replacement = ESCAPE_REPLACEMENTS[ch]
                    val data = if (replacement != null) {
                        "\\" + replacement
                    } else if (!opts.useUnicodeEncoding || !ch.isPrintable()) {
                        // if !allowUnicode or the character is not printable,
                        // we must encode it
                        if (ch <= '\u00FF') {
                            val s = "0" + ch.toInt().toString(16)
                            "\\x" + s.substring(s.length - 2)
                        } else if (ch in '\uD800'..'\uDBFF') {
                            if (end + 1 < text.length) {
                                val ch2 = text[++end]
                                val s = "000" + toCodePoint(ch, ch2).toLong().toString(16)
                                "\\U" + s.substring(s.length - 8)
                            } else {
                                val s = "000" + ch.toInt().toString(16)
                                "\\u" + s.substring(s.length - 4)
                            }
                        } else {
                            val s = "000" + ch.toInt().toString(16)
                            "\\u" + s.substring(s.length - 4)
                        }
                    } else {
                        ch.toString()
                    }
                    column += data.length
                    stream.write(data)
                    start = end + 1
                }
            }
            if (0 < end && end < text.length - 1 && (ch == ' ' || start >= end)
                && column + (end - start) > bestWidth && split
            ) {
                var data = if (start >= end) {
                    "\\"
                } else {
                    text.substring(start, end) + "\\"
                }
                if (start < end) {
                    start = end
                }
                column += data.length
                stream.write(data)
                writeIndent()
                whitespace = false
                indention = false
                if (text[start] == ' ') {
                    data = "\\"
                    column += data.length
                    stream.write(data)
                }
            }
            end += 1
        }
        writeIndicator("\"", needWhitespace = false, whitespace = false, indentation = false)
    }

    private fun determineBlockHints(text: String): String {
        val hints = StringBuilder()
        if (CharConstants.LINEBR.has(text[0].toInt(), " ")) {
            hints.append(bestIndent)
        }
        val ch1 = text[text.length - 1]
        if (CharConstants.LINEBR.hasNo(ch1.toInt())) {
            hints.append("-")
        } else if (text.length == 1 || CharConstants.LINEBR.has(text[text.length - 2].toInt())) {
            hints.append("+")
        }
        return hints.toString()
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun writeFolded(text: String, split: Boolean) {
        val hints = determineBlockHints(text)
        writeIndicator(">$hints", needWhitespace = true, whitespace = false, indentation = false)
        if (hints.isNotEmpty() && hints[hints.length - 1] == '+') {
            openEnded = true
        }
        writeLineBreak(null)
        var leadingSpace = true
        var spaces = false
        var breaks = true
        var start = 0
        var end = 0
        while (end <= text.length) {
            var ch = 0.toChar()
            if (end < text.length) {
                ch = text[end]
            }
            if (breaks) {
                if (ch.toInt() == 0 || CharConstants.LINEBR.hasNo(ch.toInt())) {
                    if (!leadingSpace && ch.toInt() != 0 && ch != ' ' && text[start] == '\n') {
                        writeLineBreak(null)
                    }
                    leadingSpace = ch == ' '
                    val data = text.substring(start, end)
                    for (br in data.toCharArray()) {
                        if (br == '\n') {
                            writeLineBreak(null)
                        } else {
                            writeLineBreak(br.toString())
                        }
                    }
                    if (ch.toInt() != 0) {
                        writeIndent()
                    }
                    start = end
                }
            } else if (spaces) {
                if (ch != ' ') {
                    if (start + 1 == end && column > bestWidth && split) {
                        writeIndent()
                    } else {
                        val len = end - start
                        column += len
                        stream.write(text, start, len)
                    }
                    start = end
                }
            } else {
                if (CharConstants.LINEBR.has(ch.toInt(), "\u0000 ")) {
                    val len = end - start
                    column += len
                    stream.write(text, start, len)
                    if (ch.toInt() == 0) {
                        writeLineBreak(null)
                    }
                    start = end
                }
            }
            if (ch.toInt() != 0) {
                breaks = CharConstants.LINEBR.has(ch.toInt())
                spaces = ch == ' '
            }
            end++
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun writeLiteral(text: String) {
        val hints = determineBlockHints(text)
        writeIndicator("|$hints", needWhitespace = true, whitespace = false, indentation = false)
        if (hints.isNotEmpty() && hints[hints.length - 1] == '+') {
            openEnded = true
        }
        writeLineBreak(null)
        var breaks = true
        var start = 0
        var end = 0
        while (end <= text.length) {
            val ch = if (end < text.length) {
                text[end]
            } else {
                0.toChar()
            }
            if (breaks) {
                if (ch.toInt() == 0 || CharConstants.LINEBR.hasNo(ch.toInt())) {
                    text.substring(start, end).forEach {
                        if (it == '\n') {
                            writeLineBreak(null)
                        } else {
                            writeLineBreak(it.toString())
                        }
                    }

                    if (ch.toInt() != 0) {
                        writeIndent()
                    }
                    start = end
                }
            } else {
                if (ch.toInt() == 0 || CharConstants.LINEBR.has(ch.toInt())) {
                    stream.write(text, start, end - start)
                    if (ch.toInt() == 0) {
                        writeLineBreak(null)
                    }
                    start = end
                }
            }
            if (ch.toInt() != 0) {
                breaks = CharConstants.LINEBR.has(ch.toInt())
            }
            end++
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun writePlain(text: String, split: Boolean) {
        if (rootContext) {
            openEnded = true
        }
        if (text.isEmpty()) {
            return
        }
        if (!whitespace) {
            column++
            stream.write(SPACE)
        }
        whitespace = false
        indention = false
        var spaces = false
        var breaks = false
        var start = 0
        var end = 0
        while (end <= text.length) {
            val ch = if (end < text.length) {
                text[end]
            } else {
                0.toChar()
            }
            if (spaces) {
                if (ch != ' ') {
                    if (start + 1 == end && column > bestWidth && split) {
                        writeIndent()
                        whitespace = false
                        indention = false
                    } else {
                        val len = end - start
                        column += len
                        stream.write(text, start, len)
                    }
                    start = end
                }
            } else if (breaks) {
                if (CharConstants.LINEBR.hasNo(ch.toInt())) {
                    if (text[start] == '\n') {
                        writeLineBreak(null)
                    }
                    text.substring(start, end).forEach {
                        if (it == '\n') {
                            writeLineBreak(null)
                        } else {
                            writeLineBreak(it.toString())
                        }
                    }

                    writeIndent()
                    whitespace = false
                    indention = false
                    start = end
                }
            } else {
                if (CharConstants.LINEBR.has(ch.toInt(), "\u0000 ")) {
                    val len = end - start
                    column += len
                    stream.write(text, start, len)
                    start = end
                }
            }
            if (ch.toInt() != 0) {
                spaces = ch == ' '
                breaks = CharConstants.LINEBR.has(ch.toInt())
            }
            end++
        }
    }
}

