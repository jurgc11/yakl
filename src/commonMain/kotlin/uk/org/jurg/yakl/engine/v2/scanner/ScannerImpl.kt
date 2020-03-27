package uk.org.jurg.yakl.engine.v2.scanner

import uk.org.jurg.yakl.engine.utils.UriEncoder.decode
import uk.org.jurg.yakl.engine.utils.codePointToString
import uk.org.jurg.yakl.engine.utils.isDigit
import uk.org.jurg.yakl.engine.utils.isSupplementaryCodePoint
import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.common.ArrayStack
import uk.org.jurg.yakl.engine.v2.common.CharConstants
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.exceptions.Mark
import uk.org.jurg.yakl.engine.v2.exceptions.ScannerException
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.tokens.AliasToken
import uk.org.jurg.yakl.engine.v2.tokens.AnchorToken
import uk.org.jurg.yakl.engine.v2.tokens.BlockEndToken
import uk.org.jurg.yakl.engine.v2.tokens.BlockEntryToken
import uk.org.jurg.yakl.engine.v2.tokens.BlockMappingStartToken
import uk.org.jurg.yakl.engine.v2.tokens.BlockSequenceStartToken
import uk.org.jurg.yakl.engine.v2.tokens.DirectiveToken
import uk.org.jurg.yakl.engine.v2.tokens.DocumentEndToken
import uk.org.jurg.yakl.engine.v2.tokens.DocumentStartToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowEntryToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowMappingEndToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowMappingStartToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowSequenceEndToken
import uk.org.jurg.yakl.engine.v2.tokens.FlowSequenceStartToken
import uk.org.jurg.yakl.engine.v2.tokens.KeyToken
import uk.org.jurg.yakl.engine.v2.tokens.ScalarToken
import uk.org.jurg.yakl.engine.v2.tokens.StreamEndToken
import uk.org.jurg.yakl.engine.v2.tokens.StreamStartToken
import uk.org.jurg.yakl.engine.v2.tokens.TAG_DIRECTIVE
import uk.org.jurg.yakl.engine.v2.tokens.TagToken
import uk.org.jurg.yakl.engine.v2.tokens.TagTuple
import uk.org.jurg.yakl.engine.v2.tokens.Token
import uk.org.jurg.yakl.engine.v2.tokens.ValueToken
import uk.org.jurg.yakl.engine.v2.tokens.YAML_DIRECTIVE
import kotlin.math.max

private const val DIRECTIVE_PREFIX = "while scanning a directive"
private const val EXPECTED_ALPHA_ERROR_PREFIX = "expected alphabetic or numeric character, but found "
private const val SCANNING_SCALAR = "while scanning a block scalar"
private const val SCANNING_PREFIX = "while scanning a "

/**
 * <pre>
 * Scanner produces tokens of the following types:
 * STREAM-START
 * STREAM-END
 * DIRECTIVE(name, value)
 * DOCUMENT-START
 * DOCUMENT-END
 * BLOCK-SEQUENCE-START
 * BLOCK-MAPPING-START
 * BLOCK-END
 * FLOW-SEQUENCE-START
 * FLOW-MAPPING-START
 * FLOW-SEQUENCE-END
 * FLOW-MAPPING-END
 * BLOCK-ENTRY
 * FLOW-ENTRY
 * KEY
 * VALUE
 * ALIAS(value)
 * ANCHOR(value)
 * TAG(value)
 * SCALAR(value, plain, style)
 * Read comments in the Scanner code for more details.
 * </pre>
 */
class ScannerImpl(private val reader: StreamReader) : Scanner {

    /**
     * A regular expression matching characters which are not in the hexadecimal
     * set (0-9, A-F, a-f).
     */
    private val NOT_HEXA = "[^0-9A-Fa-f]".toRegex()

    // Had we reached the end of the stream?
    private var done = false

    // The number of unclosed '{' and '['. `flow_level == 0` means block
    // context.
    private var flowLevel = 0

    // List of processed tokens that are not yet emitted.
    private val tokens = ArrayList<Token>(100)

    // Number of tokens that were emitted through the `get_token` method.
    private var tokensTaken = 0

    // The current indentation level.
    private var indent = -1

    // Past indentation levels.
    private val indents = ArrayStack<Int>(10)

    // Variables related to simple keys treatment.
    /**
     * <pre>
     * A simple key is a key that is not denoted by the '?' indicator.
     * Example of simple keys:
     * ---
     * block simple key: value
     * ? not a simple key:
     * : { flow simple key: value }
     * We emit the KEY token before all keys, so when we find a potential
     * simple key, we try to locate the corresponding ':' indicator.
     * Simple keys should be limited to a single line and 1024 characters.
     *
     * Can a simple key start at the current position? A simple key may
     * start:
     * - at the beginning of the line, not counting indentation spaces
     * (in block context),
     * - after '{', '[', ',' (in the flow context),
     * - after '?', ':', '-' (in the block context).
     * In the block context, this flag also signifies if a block collection
     * may start at the current position.
    </pre> *
     */
    private var allowSimpleKey = true

    /*
     * Keep track of possible simple keys. This is a dictionary. The key is
     * `flow_level`; there can be no more that one possible simple key for each
     * level. The value is a SimpleKey record: (token_number, required, index,
     * line, column, mark) A simple key may start with ALIAS, ANCHOR, TAG,
     * SCALAR(flow), '[', or '{' tokens.
     */
    private val possibleSimpleKeys = mutableMapOf<Int, SimpleKey>()

    init {
        fetchStreamStart() // Add the STREAM-START token.
    }

    /**
     * Check whether the next token is one of the given types.
     */
    override fun checkToken(vararg choices: Token.ID): Boolean {
        while (needMoreTokens()) {
            fetchMoreTokens()
        }
        if (tokens.isNotEmpty()) {
            if (choices.isEmpty()) {
                return true
            }
            // since profiler puts this method on top (it is used a lot), we
            // should not use 'foreach' here because of the performance reasons
            val firstToken = tokens[0]
            val first = firstToken.tokenId
            if (choices.any { it == first }) {
                return true
            }
        }
        return false
    }

    /**
     * Return the next token, but do not delete it from the queue.
     */
    override fun peekToken(): Token {
        while (needMoreTokens()) {
            fetchMoreTokens()
        }
        return tokens[0]
    }

    override fun hasNext(): Boolean {
        return checkToken()
    }

    /**
     * Return the next token, removing it from the queue.
     */
    override fun next(): Token {
        tokensTaken++
        return if (tokens.isEmpty()) {
            throw NoSuchElementException("No more Tokens found.")
        } else {
            tokens.removeAt(0)
        }
    }

    // Private methods.
    /**
     * Returns true if more tokens should be scanned.
     */
    private fun needMoreTokens(): Boolean {
        // If we are done, we do not require more tokens.
        if (done) {
            return false
        }
        // If we aren't done, but we have no tokens, we need to scan more.
        if (tokens.isEmpty()) {
            return true
        }
        // The current token may be a potential simple key, so we
        // need to look further.
        stalePossibleSimpleKeys()
        return nextPossibleSimpleKey() == tokensTaken
    }

    /**
     * Fetch one or more tokens from the StreamReader.
     */
    private fun fetchMoreTokens() {
        // Eat whitespaces and comments until we reach the next token.
        scanToNextToken()
        // Remove obsolete possible simple keys.
        stalePossibleSimpleKeys()
        // Compare the current indentation and column. It may add some tokens
        // and decrease the current indentation level.
        unwindIndent(reader.getColumn())
        // Peek the next code point, to decide what the next group of tokens
        // will look like.
        val c = reader.peek()
        when (c.toChar()) {
            '\u0000' -> {
                // Is it the end of stream?
                fetchStreamEnd()
                return
            }
            '%' ->                 // Is it a directive?
                if (checkDirective()) {
                    fetchDirective()
                    return
                }
            '-' ->                 // Is it the document start?
                if (checkDocumentStart()) {
                    fetchDocumentStart()
                    return
                    // Is it the block entry indicator?
                } else if (checkBlockEntry()) {
                    fetchBlockEntry()
                    return
                }
            '.' ->                 // Is it the document end?
                if (checkDocumentEnd()) {
                    fetchDocumentEnd()
                    return
                }
            '[' -> {
                // Is it the flow sequence start indicator?
                fetchFlowSequenceStart()
                return
            }
            '{' -> {
                // Is it the flow mapping start indicator?
                fetchFlowMappingStart()
                return
            }
            ']' -> {
                // Is it the flow sequence end indicator?
                fetchFlowSequenceEnd()
                return
            }
            '}' -> {
                // Is it the flow mapping end indicator?
                fetchFlowMappingEnd()
                return
            }
            ',' -> {
                // Is it the flow entry indicator?
                fetchFlowEntry()
                return
            }
            '?' ->                 // Is it the key indicator?
                if (checkKey()) {
                    fetchKey()
                    return
                }
            ':' ->                 // Is it the value indicator?
                if (checkValue()) {
                    fetchValue()
                    return
                }
            '*' -> {
                // Is it an alias?
                fetchAlias()
                return
            }
            '&' -> {
                // Is it an anchor?
                fetchAnchor()
                return
            }
            '!' -> {
                // Is it a tag?
                fetchTag()
                return
            }
            '|' ->                 // Is it a literal scalar?
                if (flowLevel == 0) {
                    fetchLiteral()
                    return
                }
            '>' ->                 // Is it a folded scalar?
                if (flowLevel == 0) {
                    fetchFolded()
                    return
                }
            '\'' -> {
                // Is it a single quoted scalar?
                fetchSingle()
                return
            }
            '"' -> {
                // Is it a double quoted scalar?
                fetchDouble()
                return
            }
            else -> {
            }
        }
        if (checkPlain()) {
            fetchPlain()
            return
        }
        // No? It's an error. Let's produce a nice error message. We do this by
        // converting escaped characters into their escape sequences. This is a
        // backwards use of the ESCAPE_REPLACEMENTS map.
        var chRepresentation = c.codePointToString()
        if (CharConstants.ESCAPES.containsKey(c.toChar())) {
            chRepresentation = "\\" + CharConstants.ESCAPES[c.toChar()]
        }
        if (c == '\t'.toInt()) {
            chRepresentation += "(TAB)"
        }
        val text =
            "found character '$chRepresentation' that cannot start any token. (Do not use $chRepresentation for indentation)"

        throw ScannerException("while scanning for the next token", null, text, reader.mark)
    }

    // Simple keys treatment.

    /**
     * Return the number of the nearest possible simple key. Actually we don't
     * need to loop through the whole dictionary.
     */
    private fun nextPossibleSimpleKey(): Int {
        /*
         * Because this.possibleSimpleKeys is ordered we can simply take the first key
         */
        return if (possibleSimpleKeys.isNotEmpty()) {
            possibleSimpleKeys.values.iterator().next().tokenNumber
        } else {
            -1
        }
    }

    /**
     * <pre>
     * Remove entries that are no longer possible simple keys. According to
     * the YAML specification, simple keys
     * - should be limited to a single line,
     * - should be no longer than 1024 characters.
     * Disabling this procedure will allow simple keys of any length and
     * height (may cause problems if indentation is broken though).
    </pre> *
     */
    private fun stalePossibleSimpleKeys() {

        val iterator = possibleSimpleKeys.values.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.line != reader.getLine() || reader.getIndex() - key.index > 1024) {
                // If the key is not on the same line as the current
                // position OR the difference in column between the token
                // start and the current position is more than the maximum
                // simple key length, then this cannot be a simple key.
                if (key.isRequired) {
                    // If the key was required, this implies an error
                    // condition.
                    throw ScannerException(
                        "while scanning a simple key", key.mark,
                        "could not find expected ':'", reader.mark
                    )
                }
                iterator.remove()
            }
        }
    }

    /**
     * The next token may start a simple key. We check if it's possible and save
     * its position. This function is called for ALIAS, ANCHOR, TAG,
     * SCALAR(flow), '[', and '{'.
     */
    private fun savePossibleSimpleKey() {
        // The next token may start a simple key. We check if it's possible
        // and save its position. This function is called for
        // ALIAS, ANCHOR, TAG, SCALAR(flow), '[', and '{'.

        // Check if a simple key is required at the current position.
        // A simple key is required if this position is the root flowLevel, AND
        // the current indentation level is the same as the last indent-level.
        val required = flowLevel == 0 && indent == reader.getColumn()
        if (allowSimpleKey || !required) {
            // A simple key is required only if it is the first token in the
            // current line. Therefore it is always allowed.
        } else {
            throw YamlEngineException("A simple key is required only if it is the first token in the current line")
        }

        // The next token might be a simple key. Let's save it's number and
        // position.
        if (allowSimpleKey) {
            removePossibleSimpleKey()
            val tokenNumber = tokensTaken + tokens.size
            val key = SimpleKey(
                tokenNumber, required, reader.getIndex(),
                reader.getLine(), reader.getColumn(), reader.mark
            )
            possibleSimpleKeys[flowLevel] = key
        }
    }

    /**
     * Remove the saved possible key position at the current flow level.
     */
    private fun removePossibleSimpleKey() {
        val key = possibleSimpleKeys.remove(flowLevel)
        if (key != null && key.isRequired) {
            throw ScannerException(
                "while scanning a simple key", key.mark,
                "could not find expected ':'", reader.mark
            )
        }
    }

    // Indentation functions.

    // Indentation functions.
    /**
     * * Handle implicitly ending multiple levels of block nodes by decreased
     * indentation. This function becomes important on lines 4 and 7 of this
     * example:
     *
     * <pre>
     * 1) book one:
     * 2)   part one:
     * 3)     chapter one
     * 4)   part two:
     * 5)     chapter one
     * 6)     chapter two
     * 7) book two:
    </pre> *
     *
     *
     * In flow context, tokens should respect indentation. Actually the
     * condition should be `self.indent &gt;= column` according to the spec. But
     * this condition will prohibit intuitively correct constructions such as
     * key : { }
     */
    private fun unwindIndent(col: Int) {
        // In the flow context, indentation is ignored. We make the scanner less
        // restrictive then specification requires.
        if (flowLevel != 0) {
            return
        }

        // In block context, we may need to issue the BLOCK-END tokens.
        while (indent > col) {
            val mark = reader.mark
            indent = indents.pop()
            tokens.add(BlockEndToken(mark, mark))
        }
    }

    /**
     * Check if we need to increase indentation.
     */
    private fun addIndent(column: Int): Boolean {
        if (indent < column) {
            indents.push(indent)
            indent = column
            return true
        }
        return false
    }

    // Fetchers.

    /**
     * We always add STREAM-START as the first token and STREAM-END as the last
     * token.
     */
    private fun fetchStreamStart() {
        // Read the token.
        val mark = reader.mark

        // Add STREAM-START.
        val token: Token = StreamStartToken(mark, mark)
        tokens.add(token)
    }

    private fun fetchStreamEnd() {
        // Set the current intendation to -1.
        unwindIndent(-1)

        // Reset simple keys.
        removePossibleSimpleKey()
        allowSimpleKey = false
        possibleSimpleKeys.clear()

        // Read the token.
        val mark = reader.mark

        // Add STREAM-END.
        tokens.add(StreamEndToken(mark, mark))

        // The stream is finished.
        done = true
    }

    /**
     * Fetch a YAML directive. Directives are presentation details that are
     * interpreted as instructions to the processor. YAML defines two kinds of
     * directives, YAML and TAG; all other types are reserved for future use.
     */
    private fun fetchDirective() {
        // Set the current intendation to -1.
        unwindIndent(-1)

        // Reset simple keys.
        removePossibleSimpleKey()
        allowSimpleKey = false

        // Scan and add DIRECTIVE.
        tokens.add(scanDirective())
    }

    /**
     * Fetch a document-start token ("---").
     */
    private fun fetchDocumentStart() {
        fetchDocumentIndicator(true)
    }

    /**
     * Fetch a document-end token ("...").
     */
    private fun fetchDocumentEnd() {
        fetchDocumentIndicator(false)
    }

    /**
     * Fetch a document indicator, either "---" for "document-start", or else
     * "..." for "document-end. The type is chosen by the given boolean.
     */
    private fun fetchDocumentIndicator(isDocumentStart: Boolean) {
        // Set the current intendation to -1.
        unwindIndent(-1)

        // Reset simple keys. Note that there could not be a block collection
        // after '---'.
        removePossibleSimpleKey()
        allowSimpleKey = false

        // Add DOCUMENT-START or DOCUMENT-END.
        val startMark = reader.mark
        reader.forward(3)
        val endMark = reader.mark
        val token = if (isDocumentStart) {
            DocumentStartToken(startMark, endMark)
        } else {
            DocumentEndToken(startMark, endMark)
        }
        tokens.add(token)
    }

    private fun fetchFlowSequenceStart() {
        fetchFlowCollectionStart(false)
    }

    private fun fetchFlowMappingStart() {
        fetchFlowCollectionStart(true)
    }

    /**
     * Fetch a flow-style collection start, which is either a sequence or a
     * mapping. The type is determined by the given boolean.
     *
     * A flow-style collection is in a format similar to JSON. Sequences are
     * started by '[' and ended by ']'; mappings are started by '{' and ended by
     * '}'.
     *
     * @param isMappingStart
     */
    private fun fetchFlowCollectionStart(isMappingStart: Boolean) {
        // '[' and '{' may start a simple key.
        savePossibleSimpleKey()

        // Increase the flow level.
        flowLevel++

        // Simple keys are allowed after '[' and '{'.
        allowSimpleKey = true

        // Add FLOW-SEQUENCE-START or FLOW-MAPPING-START.
        val startMark = reader.mark
        reader.forward(1)
        val endMark = reader.mark
        val token = if (isMappingStart) {
            FlowMappingStartToken(startMark, endMark)
        } else {
            FlowSequenceStartToken(startMark, endMark)
        }
        tokens.add(token)
    }

    private fun fetchFlowSequenceEnd() {
        fetchFlowCollectionEnd(false)
    }

    private fun fetchFlowMappingEnd() {
        fetchFlowCollectionEnd(true)
    }

    /**
     * Fetch a flow-style collection end, which is either a sequence or a
     * mapping. The type is determined by the given boolean.
     *
     *
     * A flow-style collection is in a format similar to JSON. Sequences are
     * started by '[' and ended by ']'; mappings are started by '{' and ended by
     * '}'.
     */
    private fun fetchFlowCollectionEnd(isMappingEnd: Boolean) {
        // Reset possible simple key on the current level.
        removePossibleSimpleKey()

        // Decrease the flow level.
        flowLevel--

        // No simple keys after ']' or '}'.
        allowSimpleKey = false

        // Add FLOW-SEQUENCE-END or FLOW-MAPPING-END.
        val startMark = reader.mark
        reader.forward()
        val endMark = reader.mark
        val token = if (isMappingEnd) {
            FlowMappingEndToken(startMark, endMark)
        } else {
            FlowSequenceEndToken(startMark, endMark)
        }
        tokens.add(token)
    }

    /**
     * Fetch an entry in the flow style. Flow-style entries occur either
     * immediately after the start of a collection, or else after a comma.
     */
    private fun fetchFlowEntry() {
        // Simple keys are allowed after ','.
        allowSimpleKey = true

        // Reset possible simple key on the current level.
        removePossibleSimpleKey()

        // Add FLOW-ENTRY.
        val startMark = reader.mark
        reader.forward()
        val endMark = reader.mark
        tokens.add(FlowEntryToken(startMark, endMark))
    }

    /**
     * Fetch an entry in the block style.
     */
    private fun fetchBlockEntry() {
        // Block context needs additional checks.
        if (flowLevel == 0) {
            // Are we allowed to start a new entry?
            if (!allowSimpleKey) {
                throw ScannerException(
                    "", null, "sequence entries are not allowed here",
                    reader.mark
                )
            }

            // We may need to add BLOCK-SEQUENCE-START.
            if (addIndent(reader.getColumn())) {
                val mark = reader.mark
                tokens.add(BlockSequenceStartToken(mark, mark))
            }
        } else {
            // It's an error for the block entry to occur in the flow
            // context,but we let the scanner detect this.
        }
        // Simple keys are allowed after '-'.
        allowSimpleKey = true

        // Reset possible simple key on the current level.
        removePossibleSimpleKey()

        // Add BLOCK-ENTRY.
        val startMark = reader.mark
        reader.forward()
        val endMark = reader.mark
        tokens.add(BlockEntryToken(startMark, endMark))
    }

    /**
     * Fetch a key in a block-style mapping.
     */
    private fun fetchKey() {
        // Block context needs additional checks.
        if (flowLevel == 0) {
            // Are we allowed to start a key (not necessary a simple)?
            if (!allowSimpleKey) {
                throw ScannerException(
                    problem = "mapping keys are not allowed here",
                    problemMark = reader.mark
                )
            }
            // We may need to add BLOCK-MAPPING-START.
            if (addIndent(reader.getColumn())) {
                val mark = reader.mark
                tokens.add(BlockMappingStartToken(mark, mark))
            }
        }
        // Simple keys are allowed after '?' in the block context.
        allowSimpleKey = flowLevel == 0

        // Reset possible simple key on the current level.
        removePossibleSimpleKey()

        // Add KEY.
        val startMark = reader.mark
        reader.forward()
        val endMark = reader.mark
        tokens.add(KeyToken(startMark, endMark))
    }

    /**
     * Fetch a value in a block-style mapping.
     */
    private fun fetchValue() {
        // Do we determine a simple key?
        val key = possibleSimpleKeys.remove(flowLevel)
        if (key != null) {
            // Add KEY.
            tokens.add(key.tokenNumber - tokensTaken, KeyToken(key.mark, key.mark))

            // If this key starts a new block mapping, we need to add
            // BLOCK-MAPPING-START.
            if (flowLevel == 0 && addIndent(key.column)) {
                tokens.add(key.tokenNumber - tokensTaken, BlockMappingStartToken(key.mark, key.mark))
            }
            // There cannot be two simple keys one after another.
            allowSimpleKey = false
        } else {
            // It must be a part of a complex key.
            // Block context needs additional checks. Do we really need them?
            // They will be caught by the scanner anyway.
            if (flowLevel == 0) {
                // We are allowed to start a complex value if and only if we can
                // start a simple key.
                if (!allowSimpleKey) {
                    throw ScannerException(problem = "mapping values are not allowed here", problemMark = reader.mark)
                }
            }

            // If this value starts a new block mapping, we need to add
            // BLOCK-MAPPING-START. It will be detected as an error later by
            // the scanner.
            if (flowLevel == 0 && addIndent(reader.getColumn())) {
                val mark = reader.mark
                tokens.add(BlockMappingStartToken(mark, mark))
            }

            // Simple keys are allowed after ':' in the block context.
            allowSimpleKey = flowLevel == 0

            // Reset possible simple key on the current level.
            removePossibleSimpleKey()
        }
        // Add VALUE.
        val startMark = reader.mark
        reader.forward()
        val endMark = reader.mark
        tokens.add(ValueToken(startMark, endMark))
    }

    /**
     * Fetch an alias, which is a reference to an anchor. Aliases take the
     * format:
     *
     * <pre>
     * *(anchor name)
    </pre> *
     */
    private fun fetchAlias() {
        // ALIAS could be a simple key.
        savePossibleSimpleKey()

        // No simple keys after ALIAS.
        allowSimpleKey = false

        // Scan and add ALIAS.
        tokens.add(scanAnchor(false))
    }

    /**
     * Fetch an anchor. Anchors take the form:
     *
     * <pre>
     * &(anchor name)
    </pre> *
     */
    private fun fetchAnchor() {
        // ANCHOR could start a simple key.
        savePossibleSimpleKey()

        // No simple keys after ANCHOR.
        allowSimpleKey = false

        // Scan and add ANCHOR.
        tokens.add(scanAnchor(true))
    }

    /**
     * Fetch a tag. Tags take a complex form.
     */
    private fun fetchTag() {
        // TAG could start a simple key.
        savePossibleSimpleKey()

        // No simple keys after TAG.
        allowSimpleKey = false

        // Scan and add TAG.
        tokens.add(scanTag())
    }

    /**
     * Fetch a literal scalar, denoted with a vertical-bar. This is the type
     * best used for source code and other content, such as binary data, which
     * must be included verbatim.
     */
    private fun fetchLiteral() {
        fetchBlockScalar(ScalarStyle.LITERAL)
    }

    /**
     * Fetch a folded scalar, denoted with a greater-than sign. This is the type
     * best used for long content, such as the text of a chapter or description.
     */
    private fun fetchFolded() {
        fetchBlockScalar(ScalarStyle.FOLDED)
    }

    /**
     * Fetch a block scalar (literal or folded).
     *
     * @param style
     */
    private fun fetchBlockScalar(style: ScalarStyle) {
        // A simple key may follow a block scalar.
        allowSimpleKey = true

        // Reset possible simple key on the current level.
        removePossibleSimpleKey()

        // Scan and add SCALAR.
        tokens.add(scanBlockScalar(style))
    }

    /**
     * Fetch a single-quoted (') scalar.
     */
    private fun fetchSingle() {
        fetchFlowScalar(ScalarStyle.SINGLE_QUOTED)
    }

    /**
     * Fetch a double-quoted (") scalar.
     */
    private fun fetchDouble() {
        fetchFlowScalar(ScalarStyle.DOUBLE_QUOTED)
    }

    /**
     * Fetch a flow scalar (single- or double-quoted).
     *
     * @param style
     */
    private fun fetchFlowScalar(style: ScalarStyle) {
        // A flow scalar could be a simple key.
        savePossibleSimpleKey()

        // No simple keys after flow scalars.
        allowSimpleKey = false

        // Scan and add SCALAR.
        tokens.add(scanFlowScalar(style))
    }

    /**
     * Fetch a plain scalar.
     */
    private fun fetchPlain() {
        // A plain scalar could be a simple key.
        savePossibleSimpleKey()

        // No simple keys after plain scalars. But note that `scan_plain` will
        // change this flag if the scan is finished at the beginning of the
        // line.
        allowSimpleKey = false

        // Scan and add SCALAR. May change `allow_simple_key`.
        tokens.add(scanPlain())
    }

    // Checkers.
    /**
     * Returns true if the next thing on the reader is a directive, given that
     * the leading '%' has already been checked.
     */
    private fun checkDirective(): Boolean {
        // DIRECTIVE: ^ '%' ...
        // The '%' indicator is already checked.
        return reader.getColumn() == 0
    }

    /**
     * Returns true if the next thing on the reader is a document-start ("---").
     * A document-start is always followed immediately by a new line.
     */
    private fun checkDocumentStart(): Boolean {
        // DOCUMENT-START: ^ '---' (' '|'\n')
        return if (reader.getColumn() == 0) {
            "---" == reader.prefix(3) && CharConstants.NULL_BL_T_LINEBR.has(reader.peek(3))
        } else {
            false
        }
    }

    /**
     * Returns true if the next thing on the reader is a document-end ("..."). A
     * document-end is always followed immediately by a new line.
     */
    private fun checkDocumentEnd(): Boolean {
        // DOCUMENT-END: ^ '...' (' '|'\n')
        return if (reader.getColumn() == 0) {
            "..." == reader.prefix(3) && CharConstants.NULL_BL_T_LINEBR.has(reader.peek(3))
        } else {
            false
        }
    }

    /**
     * Returns true if the next thing on the reader is a block token.
     */
    private fun checkBlockEntry(): Boolean {
        // BLOCK-ENTRY: '-' (' '|'\n')
        return CharConstants.NULL_BL_T_LINEBR.has(reader.peek(1))
    }

    /**
     * Returns true if the next thing on the reader is a key token.
     */
    private fun checkKey(): Boolean {
        // KEY(flow context): '?'
        return if (flowLevel != 0) {
            true
        } else {
            // KEY(block context): '?' (' '|'\n')
            CharConstants.NULL_BL_T_LINEBR.has(reader.peek(1))
        }
    }

    /**
     * Returns true if the next thing on the reader is a value token.
     */
    private fun checkValue(): Boolean {
        // VALUE(flow context): ':'
        return if (flowLevel != 0) {
            true
        } else {
            // VALUE(block context): ':' (' '|'\n')
            CharConstants.NULL_BL_T_LINEBR.has(reader.peek(1))
        }
    }

    /**
     * Returns true if the next thing on the reader is a plain token.
     */
    private fun checkPlain(): Boolean {
        /**
         * <pre>
         * A plain scalar may start with any non-space character except:
         * '-', '?', ':', ',', '[', ']', '{', '}',
         * '#', '&amp;', '*', '!', '|', '&gt;', '\'', '\&quot;',
         * '%', '@', '`'.
         *
         * It may also start with
         * '-', '?', ':'
         * if it is followed by a non-space character.
         *
         * Note that we limit the last rule to the block context (except the
         * '-' character) because we want the flow context to be space
         * independent.
        </pre> *
         */
        val c = reader.peek()
        // If the next char is NOT one of the forbidden chars above or
        // whitespace, then this is the start of a plain scalar.
        return (CharConstants.NULL_BL_T_LINEBR.hasNo(c, "-?:,[]{}#&*!|>\'\"%@`") || CharConstants.NULL_BL_T_LINEBR.hasNo(reader.peek(1))
                && (c == '-'.toInt() || flowLevel == 0 && "?:".indexOf(c.toChar()) != -1))
    }
// Scanners.

    // Scanners.
    /**
     * <pre>
     * We ignore spaces, line breaks and comments.
     * If we find a line break in the block context, we set the flag
     * `allow_simple_key` on.
     * The byte order mark is stripped if it's the first character in the
     * stream. We do not yet support BOM inside the stream as the
     * specification requires. Any such mark will be considered as a part
     * of the document.
     * TODO: We need to make tab handling rules more sane. A good rule is
     * Tabs cannot precede tokens
     * BLOCK-SEQUENCE-START, BLOCK-MAPPING-START, BLOCK-END,
     * KEY(block), VALUE(block), BLOCK-ENTRY
     * So the checking code is
     * if &lt;TAB&gt;:
     * self.allow_simple_keys = False
     * We also need to add the check for `allow_simple_keys == True` to
     * `unwind_indent` before issuing BLOCK-END.
     * Scanners for block, flow, and plain scalars need to be modified.
    </pre> *
     */
    private fun scanToNextToken() {
        // If there is a byte order mark (BOM) at the beginning of the stream,
        // forward past it.
        if (reader.getIndex() == 0 && reader.peek() == 0xFEFF) {
            reader.forward()
        }
        var found = false
        while (!found) {
            var ff = 0
            // Peek ahead until we find the first non-space character, then
            // move forward directly to that character.
            // (allow TAB to precede a token, test J3BT)
            while (reader.peek(ff) == ' '.toInt() || reader.peek(ff) == '\t'.toInt()) {
                ff++
            }
            if (ff > 0) {
                reader.forward(ff)
            }
            // If the character we have skipped forward to is a comment (#),
            // then peek ahead until we find the next end of line. YAML
            // comments are from a # to the next new-line. We then forward
            // past the comment.
            if (reader.peek() == '#'.toInt()) {
                ff = 0
                while (CharConstants.NULL_OR_LINEBR.hasNo(reader.peek(ff))) {
                    ff++
                }
                if (ff > 0) {
                    reader.forward(ff)
                }
            }
            // If we scanned a line break, then (depending on flow level),
            // simple keys may be allowed.
            if (scanLineBreak().isNotEmpty()) { // found a line-break
                if (flowLevel == 0) {
                    // Simple keys are allowed at flow-level 0 after a line
                    // break
                    allowSimpleKey = true
                }
            } else {
                found = true
            }
        }
    }

    private fun scanDirective(): Token {
        // See the specification for details.
        val startMark = reader.mark
        val endMark: Mark?
        reader.forward()
        val name: String = scanDirectiveName(startMark)
        val value: List<*>?
        when (name) {
            YAML_DIRECTIVE -> {
                value = scanYamlDirectiveValue(startMark)
                endMark = reader.mark
            }
            TAG_DIRECTIVE -> {
                value = scanTagDirectiveValue(startMark)
                endMark = reader.mark
            }
            else -> {
                endMark = reader.mark
                var ff = 0
                while (CharConstants.NULL_OR_LINEBR.hasNo(reader.peek(ff))) {
                    ff++
                }
                if (ff > 0) {
                    reader.forward(ff)
                }
                value = null
            }
        }
        scanDirectiveIgnoredLine(startMark)
        return DirectiveToken<Any?>(name, value, startMark, endMark)
    }

    /**
     * Scan a directive name. Directive names are a series of non-space
     * characters.
     */
    private fun scanDirectiveName(startMark: Mark?): String {
        // See the specification for details.
        var length = 0
        // A Directive-name is a sequence of alphanumeric characters
        // (a-z,A-Z,0-9). We scan until we find something that isn't.
        // This disagrees with the specification.
        var c = reader.peek(length)
        while (CharConstants.ALPHA.has(c)) {
            length++
            c = reader.peek(length)
        }
        // If the name would be empty, an error occurs.
        if (length == 0) {
            val s = c.codePointToString()
            throw ScannerException(
                DIRECTIVE_PREFIX, startMark,
                "$EXPECTED_ALPHA_ERROR_PREFIX$s($c)", reader.mark
            )
        }
        val value = reader.prefixForward(length)
        c = reader.peek()
        if (CharConstants.NULL_BL_LINEBR.hasNo(c)) {
            val s = c.codePointToString()
            throw ScannerException(
                DIRECTIVE_PREFIX, startMark,
                "$EXPECTED_ALPHA_ERROR_PREFIX$s($c)", reader.mark
            )
        }
        return value
    }

    private fun scanYamlDirectiveValue(startMark: Mark?): List<Int> {
        // See the specification for details.
        while (reader.peek() == ' '.toInt()) {
            reader.forward()
        }
        val major: Int = scanYamlDirectiveNumber(startMark)
        var c = reader.peek()
        if (c != '.'.toInt()) {
            val s = c.codePointToString()
            throw ScannerException(
                DIRECTIVE_PREFIX, startMark,
                "expected a digit or '.', but found $s($c)", reader.mark
            )
        }
        reader.forward()
        val minor: Int = scanYamlDirectiveNumber(startMark)
        c = reader.peek()
        if (CharConstants.NULL_BL_LINEBR.hasNo(c)) {
            val s = c.codePointToString()
            throw ScannerException(
                DIRECTIVE_PREFIX, startMark,
                "expected a digit or ' ', but found " + s + "("
                        + c + ")", reader.mark
            )
        }
        return listOf(major, minor)
    }

    /**
     * Read a %YAML directive number: this is either the major or the minor
     * part. Stop reading at a non-digit character (usually either '.' or '\n').
     */
    private fun scanYamlDirectiveNumber(startMark: Mark?): Int {
        // See the specification for details.
        val c = reader.peek()
        if (!c.isDigit()) {
            val s = c.codePointToString()
            throw ScannerException(
                DIRECTIVE_PREFIX, startMark,
                "expected a digit, but found $s($c)", reader.mark
            )
        }
        var length = 0
        while (reader.peek(length).isDigit()) {
            length++
        }
        return reader.prefixForward(length).toInt()
    }

    /**
     * Read a %TAG directive value:
     * <pre>
     * s-ignored-space+ c-tag-handle s-ignored-space+ ns-tag-prefix s-l-comments
     * </pre>
     */
    private fun scanTagDirectiveValue(startMark: Mark?): List<String> {
        // See the specification for details.
        while (reader.peek() == ' '.toInt()) {
            reader.forward()
        }
        val handle = scanTagDirectiveHandle(startMark)
        while (reader.peek() == ' '.toInt()) {
            reader.forward()
        }
        val prefix = scanTagDirectivePrefix(startMark)
        return listOf(handle, prefix)
    }

    /**
     * Scan a %TAG directive's handle. This is YAML's c-tag-handle.
     *
     * @param startMark
     * @return the directive value
     */
    private fun scanTagDirectiveHandle(startMark: Mark?): String {
        // See the specification for details.
        val value: String = scanTagHandle("directive", startMark)
        val c = reader.peek()
        if (c != ' '.toInt()) {
            val s = c.codePointToString()
            throw ScannerException(
                DIRECTIVE_PREFIX, startMark,
                "expected ' ', but found $s($c)", reader.mark
            )
        }
        return value
    }

    /**
     * Scan a %TAG directive's prefix. This is YAML's ns-tag-prefix.
     */
    private fun scanTagDirectivePrefix(startMark: Mark?): String {
        // See the specification for details.
        val value = scanTagUri("directive", startMark)
        val c = reader.peek()
        if (CharConstants.NULL_BL_LINEBR.hasNo(c)) {
            val s =c.codePointToString()
            throw ScannerException(
                DIRECTIVE_PREFIX, startMark,
                "expected ' ', but found $s($c)",
                reader.mark
            )
        }
        return value
    }

    private fun scanDirectiveIgnoredLine(startMark: Mark?) {
        // See the specification for details.
        while (reader.peek() == ' '.toInt()) {
            reader.forward()
        }
        if (reader.peek() == '#'.toInt()) {
            while (CharConstants.NULL_OR_LINEBR.hasNo(reader.peek())) {
                reader.forward()
            }
        }
        val c = reader.peek()
        val lineBreak = scanLineBreak()
        if (lineBreak.isEmpty() && c != '\u0000'.toInt()) {
            val s = c.codePointToString()
            throw ScannerException(
                DIRECTIVE_PREFIX, startMark,
                "expected a comment or a line break, but found $s($c)",
                reader.mark
            )
        }
    }

    private fun scanAnchor(isAnchor: Boolean): Token {
        val startMark = reader.mark
        val name = if (reader.peek() == '*'.toInt()) "alias" else "anchor"
        reader.forward()
        var length = 0
        var c = reader.peek(length)
        // Anchor may not contain ",[]{}", the ":" was added by SnakeYAML -> should it be added to the spec 1.2 ?
        while (CharConstants.NULL_BL_T_LINEBR.hasNo(c, ":,[]{}")) {
            length++
            c = reader.peek(length)
        }
        if (length == 0) {
            val s = c.codePointToString()
            throw ScannerException(
                "while scanning an $name", startMark,
                "unexpected character found $s($c)", reader.mark
            )
        }
        val value = reader.prefixForward(length)
        c = reader.peek()
        if (CharConstants.NULL_BL_T_LINEBR.hasNo(c, "?:,]}%@`")) {
            val s = c.codePointToString()
            throw ScannerException(
                "while scanning an $name", startMark,
                "unexpected character found $s($c)", reader.mark
            )
        }
        val endMark = reader.mark
        return if (isAnchor) {
            AnchorToken(Anchor(value), startMark, endMark)
        } else {
            AliasToken(Anchor(value), startMark, endMark)
        }
    }

    /**
     * Scan a Tag property. A Tag property may be specified in one of three
     * ways: c-verbatim-tag, c-ns-shorthand-tag, or c-ns-non-specific-tag
     *
     * c-verbatim-tag takes the form !&lt;ns-uri-char+&gt; and must be delivered
     * verbatim (as-is) to the application. In particular, verbatim tags are not
     * subject to tag resolution.
     *
     * c-ns-shorthand-tag is a valid tag handle followed by a non-empty suffix.
     * If the tag handle is a c-primary-tag-handle ('!') then the suffix must
     * have all exclamation marks properly URI-escaped (%21); otherwise, the
     * string will look like a named tag handle: !foo!bar would be interpreted
     * as (handle="!foo!", suffix="bar").
     *
     * c-ns-non-specific-tag is always a lone '!'; this is only useful for plain
     * scalars, where its specification means that the scalar MUST be resolved
     * to have type tag:yaml.org,2002:str.
     *
     * TODO SnakeYAML incorrectly ignores c-ns-non-specific-tag right now.
     * TODO Note that this method does not enforce rules about local versus global tags!
     */
    private fun scanTag(): Token {
        // See the specification for details.
        val startMark = reader.mark
        // Determine the type of tag property based on the first character
        // encountered
        var c = reader.peek(1)
        var handle: String? = null
        val suffix: String
        // Verbatim tag! (c-verbatim-tag)
        if (c == '<'.toInt()) {
            // Skip the exclamation mark and &gt;, then read the tag suffix (as
            // a URI).
            reader.forward(2)
            suffix = scanTagUri("tag", startMark)
            c = reader.peek()
            if (c != '>'.toInt()) {
                // If there are any characters between the end of the tag-suffix
                // URI and the closing &gt;, then an error has occurred.
                val s = c.codePointToString()
                throw ScannerException(
                    "while scanning a tag", startMark,
                    "expected '>', but found '$s' ($c)", reader.mark
                )
            }
            reader.forward()
        } else if (CharConstants.NULL_BL_T_LINEBR.has(c)) {
            // A NUL, blank, tab, or line-break means that this was a
            // c-ns-non-specific tag.
            suffix = "!"
            reader.forward()
        } else {
            // Any other character implies c-ns-shorthand-tag type.

            // Look ahead in the stream to determine whether this tag property
            // is of the form !foo or !foo!bar.
            var length = 1
            var useHandle = false
            while (CharConstants.NULL_BL_LINEBR.hasNo(c)) {
                if (c == '!'.toInt()) {
                    useHandle = true
                    break
                }
                length++
                c = reader.peek(length)
            }
            // If we need to use a handle, scan it in; otherwise, the handle is
            // presumed to be '!'.
            if (useHandle) {
                handle = scanTagHandle("tag", startMark)
            } else {
                handle = "!"
                reader.forward()
            }
            suffix = scanTagUri("tag", startMark)
        }
        c = reader.peek()
        // Check that the next character is allowed to follow a tag-property, if it is not, raise the error.
        if (CharConstants.NULL_BL_LINEBR.hasNo(c)) {
            val s = c.codePointToString()
            throw ScannerException(
                "while scanning a tag", startMark,
                "expected ' ', but found '$s' ($c)", reader.mark
            )
        }
        val value = TagTuple(handle, suffix)
        val endMark = reader.mark
        return TagToken(value, startMark, endMark)
    }

    private fun scanBlockScalar(style: ScalarStyle): Token {
        // See the specification for details.
        val chunks = StringBuilder()
        val startMark = reader.mark
        // Scan the header.
        reader.forward()
        val chomping = scanBlockScalarIndicators(startMark)
        val increment = chomping.increment
        scanBlockScalarIgnoredLine(startMark)

        // Determine the indentation level and go to the first non-empty line.
        val minIndent = max(1, indent+1)

        var breaks: String
        val blockIndent: Int
        var endMark: Mark?
        if (increment == -1) {
            val brme = scanBlockScalarIndentation()
            breaks = brme.first
            endMark = brme.third
            blockIndent = max(minIndent, brme.second)
        } else {
            blockIndent = minIndent + increment - 1
            val brme = scanBlockScalarBreaks(blockIndent)
            breaks = brme.first
            endMark = brme.second
        }
        var lineBreak = ""

        // Scan the inner part of the block scalar.
        while (reader.getColumn() == blockIndent && reader.peek() != '\u0000'.toInt()) {
            chunks.append(breaks)
            val leadingNonSpace = !" \t".contains(reader.peek().toChar())
            var length = 0
            while (CharConstants.NULL_OR_LINEBR.hasNo(reader.peek(length))) {
                length++
            }
            chunks.append(reader.prefixForward(length))
            lineBreak = scanLineBreak()
            val brme = scanBlockScalarBreaks(blockIndent)
            breaks = brme.first
            endMark = brme.second
            if (reader.getColumn() == blockIndent && reader.peek() != '\u0000'.toInt()) {

                // Unfortunately, folding rules are ambiguous.
                //
                // This is the folding according to the specification:
                if (style === ScalarStyle.FOLDED && "\n" == lineBreak && leadingNonSpace
                    && !" \t".contains(reader.peek().toChar())) {
                    if (breaks.isEmpty()) {
                        chunks.append(" ")
                    }
                } else {
                    chunks.append(lineBreak)
                }
            } else {
                break
            }
        }
        // Chomp the tail.
        if (chomping.chompTailIsNotFalse()) {
            chunks.append(lineBreak)
        }
        if (chomping.chompTailIsTrue()) {
            chunks.append(breaks)
        }
        // We are done.
        return ScalarToken(chunks.toString(), false, style, startMark, endMark)
    }

    /**
     * Scan a block scalar indicator. The block scalar indicator includes two
     * optional components, which may appear in either order.
     *
     * A block indentation indicator is a non-zero digit describing the
     * indentation level of the block scalar to follow. This indentation is an
     * additional number of spaces relative to the current indentation level.
     *
     * A block chomping indicator is a + or -, selecting the chomping mode away
     * from the default (clip) to either -(strip) or +(keep).
     */
    private fun scanBlockScalarIndicators(startMark: Mark?): Chomping {
        // See the specification for details.
        var chomping: Boolean? = null
        var increment = -1
        var c = reader.peek()
        if (c == '-'.toInt() || c == '+'.toInt()) {
            chomping = c == '+'.toInt()
            reader.forward()
            c = reader.peek()
            if (c.isDigit()) {
                val s = c.codePointToString()
                increment = s.toInt()
                if (increment == 0) {
                    throw ScannerException(
                        SCANNING_SCALAR, startMark,
                        "expected indentation indicator in the range 1-9, but found 0",
                        reader.mark
                    )
                }
                reader.forward()
            }
        } else if (c.isDigit()) {
            val s = c.codePointToString()
            increment = s.toInt()
            if (increment == 0) {
                throw ScannerException(
                    SCANNING_SCALAR, startMark,
                    "expected indentation indicator in the range 1-9, but found 0",
                    reader.mark
                )
            }
            reader.forward()
            c = reader.peek()
            if (c == '-'.toInt() || c == '+'.toInt()) {
                chomping = c == '+'.toInt()
                reader.forward()
            }
        }
        c = reader.peek()
        if (CharConstants.NULL_BL_LINEBR.hasNo(c)) {
            val s = c.codePointToString()
            throw ScannerException(
                SCANNING_SCALAR, startMark,
                "expected chomping or indentation indicators, but found $s($c)", reader.mark
            )
        }
        return Chomping(chomping, increment)
    }

    /**
     * Scan to the end of the line after a block scalar has been scanned; the
     * only things that are permitted at this time are comments and spaces.
     */
    private fun scanBlockScalarIgnoredLine(startMark: Mark?): String {
        // See the specification for details.

        // Forward past any number of trailing spaces
        while (reader.peek() == ' '.toInt()) {
            reader.forward()
        }

        // If a comment occurs, scan to just before the end of line.
        if (reader.peek() == '#'.toInt()) {
            while (CharConstants.NULL_OR_LINEBR.hasNo(reader.peek())) {
                reader.forward()
            }
        }
        // If the next character is not a null or line break, an error has
        // occurred.
        val c = reader.peek()
        val lineBreak: String = scanLineBreak()
        if (lineBreak.isEmpty() && c != '\u0000'.toInt()) {
            val s = c.codePointToString()
            throw ScannerException(
                SCANNING_SCALAR, startMark,
                "expected a comment or a line break, but found $s($c)", reader.mark
            )
        }
        return lineBreak
    }

    /**
     * Scans for the indentation of a block scalar implicitly. This mechanism is
     * used only if the block did not explicitly state an indentation to be
     * used.
     */
    private fun scanBlockScalarIndentation(): Triple<String, Int, Mark?> {
        // See the specification for details.
        val chunks = StringBuilder()
        var maxIndent = 0
        var endMark = reader.mark
        // Look ahead some number of lines until the first non-blank character
        // occurs; the determined indentation will be the maximum number of
        // leading spaces on any of these lines.
        while (CharConstants.LINEBR.has(reader.peek(), " \r")) {
            if (reader.peek() != ' '.toInt()) {
                // If the character isn't a space, it must be some kind of
                // line-break; scan the line break and track it.
                chunks.append(scanLineBreak())
                endMark = reader.mark
            } else {
                // If the character is a space, move forward to the next
                // character; if we surpass our previous maximum for indent
                // level, update that too.
                reader.forward()
                if (reader.getColumn() > maxIndent) {
                    maxIndent = reader.getColumn()
                }
            }
        }
        // Pass several results back together.
        return Triple(chunks.toString(), maxIndent, endMark)
    }

    private fun scanBlockScalarBreaks(indent: Int): Pair<String, Mark?> {
        // See the specification for details.
        val chunks = StringBuilder()
        var endMark = reader.mark
        var col = reader.getColumn()
        // Scan for up to the expected indentation-level of spaces, then move
        // forward past that amount.
        while (col < indent && reader.peek() == ' '.toInt()) {
            reader.forward()
            col++
        }

        // Consume one or more line breaks followed by any amount of spaces,
        // until we find something that isn't a line-break.
        var lineBreak: String
        while (scanLineBreak().also { lineBreak = it }.isNotEmpty()) {
            chunks.append(lineBreak)
            endMark = reader.mark
            // Scan past up to (indent) spaces on the next line, then forward
            // past them.
            col = reader.getColumn()
            while (col < indent && reader.peek() == ' '.toInt()) {
                reader.forward()
                col++
            }
        }
        // Return both the assembled intervening string and the end-mark.
        return chunks.toString() to endMark
    }

    /**
     * Scan a flow-style scalar. Flow scalars are presented in one of two forms;
     * first, a flow scalar may be a double-quoted string; second, a flow scalar
     * may be a single-quoted string.
     *
     *
     * <pre>
     * See the specification for details.
     * Note that we loose indentation rules for quoted scalars. Quoted
     * scalars don't need to adhere indentation because &quot; and ' clearly
     * mark the beginning and the end of them. Therefore we are less
     * restrictive then the specification requires. We only need to check
     * that document separators are not included in scalars.
    </pre> *
     */
    private fun scanFlowScalar(style: ScalarStyle): Token {
        // The style will be either single- or double-quoted; we determine this
        // by the first character in the entry (supplied)
        val doubleValue = style === ScalarStyle.DOUBLE_QUOTED
        val chunks = StringBuilder()
        val startMark = reader.mark
        val quote = reader.peek()
        reader.forward()
        chunks.append(scanFlowScalarNonSpaces(doubleValue, startMark))
        while (reader.peek() != quote) {
            chunks.append(scanFlowScalarSpaces(startMark))
            chunks.append(scanFlowScalarNonSpaces(doubleValue, startMark))
        }
        reader.forward()
        val endMark = reader.mark
        return ScalarToken(chunks.toString(), false, style, startMark, endMark)
    }

    /**
     * Scan some number of flow-scalar non-space characters.
     */
    private fun scanFlowScalarNonSpaces(doubleQuoted: Boolean, startMark: Mark?): String {
        // See the specification for details.
        val chunks = StringBuilder()
        while (true) {
            // Scan through any number of characters which are not: NUL, blank,
            // tabs, line breaks, single-quotes, double-quotes, or backslashes.
            var length = 0
            while (CharConstants.NULL_BL_T_LINEBR.hasNo(reader.peek(length), "\'\"\\")) {
                length++
            }
            if (length != 0) {
                chunks.append(reader.prefixForward(length))
            }
            // Depending on our quoting-type, the characters ', " and \ have
            // differing meanings.
            var c = reader.peek()
            if (!doubleQuoted && c == '\''.toInt() && reader.peek(1) == '\''.toInt()) {
                chunks.append("'")
                reader.forward(2)
            } else if (doubleQuoted && c == '\''.toInt() || !doubleQuoted && "\"\\".indexOf(c.toChar()) != -1) {
                chunks.append(c.codePointToString())
                reader.forward()
            } else if (doubleQuoted && c == '\\'.toInt()) {
                reader.forward()
                c = reader.peek()
                if (!c.isSupplementaryCodePoint() && CharConstants.ESCAPE_REPLACEMENTS.containsKey(c)) {
                    // The character is one of the single-replacement
                    // types; these are replaced with a literal character
                    // from the mapping.
                    chunks.append(CharConstants.ESCAPE_REPLACEMENTS[c])
                    reader.forward()
                } else if (!c.isSupplementaryCodePoint() && CharConstants.ESCAPE_CODES.containsKey(c.toChar())) {
                    // The character is a multi-digit escape sequence, with
                    // length defined by the value in the ESCAPE_CODES map.
                    length = CharConstants.ESCAPE_CODES.getValue(c.toChar())
                    reader.forward()
                    val hex = reader.prefix(length)
                    if (NOT_HEXA.matches(hex)) {
                        throw ScannerException(
                            "while scanning a double-quoted scalar",
                            startMark, "expected escape sequence of $length hexadecimal numbers, but found: $hex",
                            reader.mark
                        )
                    }
                    val decimal = hex.toInt(16)
                    val unicode = decimal.codePointToString()
                    chunks.append(unicode)
                    reader.forward(length)
                } else if (scanLineBreak().isNotEmpty()) {
                    chunks.append(scanFlowScalarBreaks(startMark))
                } else {
                    val s = c.codePointToString()
                    throw ScannerException(
                        "while scanning a double-quoted scalar", startMark,
                        "found unknown escape character $s($c)",
                        reader.mark
                    )
                }
            } else {
                return chunks.toString()
            }
        }
    }

    private fun scanFlowScalarSpaces(startMark: Mark?): String {
        // See the specification for details.
        val chunks = StringBuilder()
        var length = 0
        // Scan through any number of whitespace (space, tab) characters,
        // consuming them.
        while (" \t".indexOf(reader.peek(length).toChar()) != -1) {
            length++
        }
        val whitespaces = reader.prefixForward(length)
        val c = reader.peek()
        if (c == '\u0000'.toInt()) {
            // A flow scalar cannot end with an end-of-stream
            throw ScannerException(
                "while scanning a quoted scalar", startMark,
                "found unexpected end of stream", reader.mark
            )
        }
        // If we encounter a line break, scan it into our assembled string...
        val lineBreak = scanLineBreak()
        if (lineBreak.isNotEmpty()) {
            val breaks = scanFlowScalarBreaks(startMark)
            if ("\n" != lineBreak) {
                chunks.append(lineBreak)
            } else if (breaks.isEmpty()) {
                chunks.append(" ")
            }
            chunks.append(breaks)
        } else {
            chunks.append(whitespaces)
        }
        return chunks.toString()
    }

    private fun scanFlowScalarBreaks(startMark: Mark?): String {
        // See the specification for details.
        val chunks = StringBuilder()
        while (true) {
            // Instead of checking indentation, we check for document
            // separators.
            val prefix = reader.prefix(3)
            if (("---" == prefix || "..." == prefix) && CharConstants.NULL_BL_T_LINEBR.has(reader.peek(3))) {
                throw ScannerException(
                    "while scanning a quoted scalar", startMark,
                    "found unexpected document separator", reader.mark
                )
            }
            // Scan past any number of spaces and tabs, ignoring them
            while (" \t".indexOf(reader.peek().toChar()) != -1) {
                reader.forward()
            }
            // If we stopped at a line break, add that; otherwise, return the
            // assembled set of scalar breaks.
            val lineBreak = scanLineBreak()
            if (lineBreak.isNotEmpty()) {
                chunks.append(lineBreak)
            } else {
                return chunks.toString()
            }
        }
    }

    /**
     * Scan a plain scalar.
     *
     * <pre>
     * See the specification for details.
     * We add an additional restriction for the flow context:
     * plain scalars in the flow context cannot contain ',', ':' and '?'.
     * We also keep track of the `allow_simple_key` flag here.
     * Indentation rules are loosed for the flow context.
    </pre> *
     */
    private fun scanPlain(): Token {
        val chunks = StringBuilder()
        val startMark = reader.mark
        var endMark = startMark
        val plainIndent = indent + 1
        var spaces = ""
        while (true) {
            var c: Int
            var length = 0
            // A comment indicates the end of the scalar.
            if (reader.peek() == '#'.toInt()) {
                break
            }
            while (true) {
                c = reader.peek(length)
                val nullBlTLineBr = CharConstants.NULL_BL_T_LINEBR.has(
                    reader.peek(length + 1),
                    if (flowLevel != 0) ",[]{}" else ""
                )
                if (CharConstants.NULL_BL_T_LINEBR.has(c) ||
                    c == ':'.toInt() && nullBlTLineBr ||
                    flowLevel != 0 && ",?[]{}".indexOf(c.toChar()) != -1
                ) {
                    break
                }
                length++
            }
            if (length == 0) {
                break
            }
            allowSimpleKey = false
            chunks.append(spaces)
            chunks.append(reader.prefixForward(length))
            endMark = reader.mark
            spaces = scanPlainSpaces()
            if (spaces.isEmpty() || reader.peek() == '#'.toInt() || flowLevel == 0 && reader.getColumn() < plainIndent) {
                break
            }
        }
        return ScalarToken(chunks.toString(), true, startMark = startMark, endMark = endMark)
    }

    /**
     * See the specification for details. SnakeYAML and libyaml allow tabs
     * inside plain scalar
     */
    private fun scanPlainSpaces(): String {
        var length = 0
        while (reader.peek(length) == ' '.toInt() || reader.peek(length) == '\t'.toInt()) {
            length++
        }
        val whitespaces = reader.prefixForward(length)
        val lineBreak = scanLineBreak()
        if (lineBreak.isNotEmpty()) {
            allowSimpleKey = true
            var prefix = reader.prefix(3)
            if ("---" == prefix || "..." == prefix && CharConstants.NULL_BL_T_LINEBR.has(reader.peek(3))) {
                return ""
            }
            val breaks = StringBuilder()
            while (true) {
                if (reader.peek() == ' '.toInt()) {
                    reader.forward()
                } else {
                    val lb = scanLineBreak()
                    if (lb.isNotEmpty()) {
                        breaks.append(lb)
                        prefix = reader.prefix(3)
                        if ("---" == prefix || "..." == prefix && CharConstants.NULL_BL_T_LINEBR.has(reader.peek(3))) {
                            return ""
                        }
                    } else {
                        break
                    }
                }
            }
            return when {
                "\n" != lineBreak -> lineBreak + breaks
                breaks.isEmpty() -> " "
                else -> breaks.toString()
            }
        }
        return whitespaces
    }

    /**
     *
     *
     * Scan a Tag handle. A Tag handle takes one of three forms:
     *
     *
     * <pre>
     * "!" (c-primary-tag-handle)
     * "!!" (ns-secondary-tag-handle)
     * "!(name)!" (c-named-tag-handle)
    </pre> *
     *
     *
     * Where (name) must be formatted as an ns-word-char.
     *
     *
     *
     * <pre>
     * See the specification for details.
     * For some strange reasons, the specification does not allow '_' in
     * tag handles. I have allowed it anyway.
    </pre> *
     */
    private fun scanTagHandle(name: String, startMark: Mark?): String {
        var c = reader.peek()
        if (c != '!'.toInt()) {
            val s = c.codePointToString()
            throw ScannerException(
                SCANNING_PREFIX + name, startMark,
                "expected '!', but found $s($c)", reader.mark
            )
        }
        // Look for the next '!' in the stream, stopping if we hit a
        // non-word-character. If the first character is a space, then the
        // tag-handle is a c-primary-tag-handle ('!').
        var length = 1
        c = reader.peek(length)
        if (c != ' '.toInt()) {
            // Scan through 0+ alphabetic characters.
            // According to the specification, these should be
            // ns-word-char only, which prohibits '_'. This might be a
            // candidate for a configuration option.
            while (CharConstants.ALPHA.has(c)) {
                length++
                c = reader.peek(length)
            }
            // Found the next non-word-char. If this is not a space and not an
            // '!', then this is an error, as the tag-handle was specified as:
            // !(name) or similar; the trailing '!' is missing.
            if (c != '!'.toInt()) {
                reader.forward(length)
                val s = c.codePointToString()
                throw ScannerException(
                    SCANNING_PREFIX + name, startMark,
                    "expected '!', but found $s($c)", reader.mark
                )
            }
            length++
        }
        return reader.prefixForward(length)
    }

    /**
     *
     *
     * Scan a Tag URI. This scanning is valid for both local and global tag
     * directives, because both appear to be valid URIs as far as scanning is
     * concerned. The difference may be distinguished later, in parsing. This
     * method will scan for ns-uri-char*, which covers both cases.
     *
     *
     *
     *
     *
     * This method performs no verification that the scanned URI conforms to any
     * particular kind of URI specification.
     *
     */
    private fun scanTagUri(name: String, startMark: Mark?): String {
        // See the specification for details.
        // Note: we do not check if URI is well-formed.
        val chunks = StringBuilder()
        // Scan through accepted URI characters, which includes the standard
        // URI characters, plus the start-escape character ('%'). When we get
        // to a start-escape, scan the escaped sequence, then return.
        var length = 0
        var c = reader.peek(length)
        while (CharConstants.URI_CHARS.has(c)) {
            if (c == '%'.toInt()) {
                chunks.append(reader.prefixForward(length))
                length = 0
                chunks.append(scanUriEscapes(name, startMark))
            } else {
                length++
            }
            c = reader.peek(length)
        }
        // Consume the last "chunk", which would not otherwise be consumed by
        // the loop above.
        if (length != 0) {
            chunks.append(reader.prefixForward(length))
        }
        if (chunks.isEmpty()) {
            // If no URI was found, an error has occurred.
            val s = c.codePointToString()
            throw ScannerException(
                SCANNING_PREFIX + name, startMark,
                "expected URI, but found $s($c)", reader.mark
            )
        }
        return chunks.toString()
    }

    /**
     *
     *
     * Scan a sequence of %-escaped URI escape codes and convert them into a
     * String representing the unescaped values.
     *
     *
     *
     * This method fails for more than 256 bytes' worth of URI-encoded
     * characters in a row. Is this possible? Is this a use-case?
     */

    @OptIn(ExperimentalStdlibApi::class)
    private fun scanUriEscapes(
        name: String,
        startMark: Mark?
    ): String {
        // First, look ahead to see how many URI-escaped characters we should
        // expect, so we can use the correct buffer size.
        var length = 1
        while (reader.peek(length * 3) == '%'.toInt()) {
            length++
        }
        // See the specification for details.
        // URIs containing 16 and 32 bit Unicode characters are
        // encoded in UTF-8, and then each octet is written as a
        // separate character.
        val beginningMark = reader.mark
        val buff = ByteArray(length)
        var i = 0
        while (reader.peek() == '%'.toInt()) {
            reader.forward()
            try {
                buff[i] = reader.prefix(2).toInt(16).toByte()
                i++
            } catch (nfe: NumberFormatException) {
                val c1 = reader.peek()
                val s1 = c1.codePointToString()
                val c2 = reader.peek(1)
                val s2 =c2.codePointToString()
                throw ScannerException(
                    SCANNING_PREFIX + name, startMark,
                    "expected URI escape sequence of 2 hexadecimal numbers, but found $s1($c1) and $s2($c2)",
                    reader.mark
                )
            }
            reader.forward(2)
        }
        return try {
            decode(buff.decodeToString(0, i, true))
        } catch (e: CharacterCodingException) {
            throw ScannerException(
                SCANNING_PREFIX + name, startMark,
                "expected URI in UTF-8: " + e.message, beginningMark
            )
        }
    }

    /**
     * Scan a line break, transforming:
     *
     * <pre>
     * '\r\n' : '\n'
     * '\r' : '\n'
     * '\n' : '\n'
     * '\x85' : '\n'
     * default : ''
    </pre> *
     */
    private fun scanLineBreak(): String {
        // Transforms:
        // '\r\n' : '\n'
        // '\r' : '\n'
        // '\n' : '\n'
        // '\x85' : '\n'
        // default : ''
        val c = reader.peek()
        if (c == '\r'.toInt() || c == '\n'.toInt() || c == '\u0085'.toInt()) {
            if (c == '\r'.toInt() && '\n'.toInt() == reader.peek(1)) {
                reader.forward(2)
            } else {
                reader.forward()
            }
            return "\n"
        } else if (c == '\u2028'.toInt() || c == '\u2029'.toInt()) {
            reader.forward()
            return c.codePointToString()
        }
        return ""
    }

    /**
     * Chomping the tail may have 3 values - yes, no, not defined.
     */
    private class Chomping(private val value: Boolean?, val increment: Int) {

        fun chompTailIsNotFalse(): Boolean {
            return value == null || value
        }

        fun chompTailIsTrue(): Boolean {
            return value != null && value
        }

    }
}
