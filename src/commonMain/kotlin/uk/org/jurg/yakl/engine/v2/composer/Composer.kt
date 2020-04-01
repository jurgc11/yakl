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
package uk.org.jurg.yakl.engine.v2.composer

import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.events.AliasEvent
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.events.MappingStartEvent
import uk.org.jurg.yakl.engine.v2.events.NodeEvent
import uk.org.jurg.yakl.engine.v2.events.ScalarEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceStartEvent
import uk.org.jurg.yakl.engine.v2.exceptions.ComposerException
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.MappingNode
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.nodes.NodeTuple
import uk.org.jurg.yakl.engine.v2.nodes.NodeType
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import uk.org.jurg.yakl.engine.v2.nodes.SequenceNode
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.parser.Parser
import uk.org.jurg.yakl.engine.v2.resolver.ScalarResolver

/**
 * Creates a node graph from parser events.
 *
 *
 * Corresponds to the 'Composer' step as described in chapter 3.1.2 of the
 * [YAML Specification](http://www.yaml.org/spec/1.2/spec.html#id2762107).
 *
 */
class Composer(
    private val parser: Parser,
    private val settings: LoadSettings) : Iterator<Node> {

    private val scalarResolver: ScalarResolver = settings.scalarResolver
    private val anchors: MutableMap<Anchor, Node> = mutableMapOf()
    private val recursiveNodes: MutableSet<Node> = mutableSetOf()
    private var nonScalarAliasesCount = 0

    /**
     * Checks if further documents are available.
     *
     * @return `true` if there is at least one more document.
     */
    override fun hasNext(): Boolean {
        // Drop the STREAM-START event.
        if (parser.checkEvent(Event.ID.StreamStart)) {
            parser.next()
        }
        // If there are more documents available?
        return !parser.checkEvent(Event.ID.StreamEnd)
    }

    /**
     * Reads a document from a source that contains only one document.
     *
     * If the stream contains more than one document an exception is thrown.
     *
     * @return The root node of the document or `null` if no document
     * is available.
     */
    fun getSingleNode(): Node? {
        // Drop the STREAM-START event.
        parser.next()
        // Compose a document if the stream is not empty.
        var document: Node? = null
        if (!parser.checkEvent(Event.ID.StreamEnd)) {
            document = next()
        }
        // Ensure that the stream contains no more documents.
        if (!parser.checkEvent(Event.ID.StreamEnd)) {
            val event = parser.next()
            throw ComposerException(
                "expected a single document in the stream",
                document?.startMark,
                "but found another document",
                event.startMark
            )
        }
        // Drop the STREAM-END event.
        parser.next()
        return document
    }

    /**
     * Reads and composes the next document.
     *
     * @return The root node of the document or `null` if no more
     * documents are available.
     */
    override fun next(): Node { // Drop the DOCUMENT-START event.
        parser.next()
        // Compose the root node.
        val node: Node = composeNode(null)
        // Drop the DOCUMENT-END event.
        parser.next()
        anchors.clear()
        recursiveNodes.clear()
        nonScalarAliasesCount = 0
        return node
    }

    private fun composeNode(parent: Node?): Node {
        if (parent != null) {
            recursiveNodes.add(parent)
        }

        val node: Node?
        if (parser.checkEvent(Event.ID.Alias)) {
            val event = parser.next() as AliasEvent
            val anchor = event.alias
            node = anchors[anchor] ?: throw ComposerException(problem = "found undefined alias $anchor", problemMark = event.startMark)
            if (node.nodeType != NodeType.SCALAR) {
                nonScalarAliasesCount++
                if (nonScalarAliasesCount > settings.maxAliasesForCollections) {
                    throw YamlEngineException("Number of aliases for non-scalar nodes exceeds the specified max=" + settings.maxAliasesForCollections)
                }
            }
            if (recursiveNodes.remove(node)) {
                node.recursive = true
            }
        } else {
            val event = parser.peekEvent() as NodeEvent
            val anchor = event.anchor
            // the check for duplicate anchors has been removed (issue 174)
            node = when {
                parser.checkEvent(Event.ID.Scalar) -> composeScalarNode(anchor)
                parser.checkEvent(Event.ID.SequenceStart) -> composeSequenceNode(anchor)
                else -> composeMappingNode(anchor)
            }
        }
        if(parent != null) {
            recursiveNodes.remove(parent)
        }

        return node
    }

    private fun registerAnchor(anchor: Anchor, node: Node) {
        anchors[anchor] = node
        node.anchor = anchor
    }

    private fun composeScalarNode(anchor: Anchor?): Node {
        val ev = parser.next() as ScalarEvent
        var resolved = false
        val nodeTag = if (ev.tag == null || ev.tag == "!") {
            resolved = true
            scalarResolver.resolve(ev.value, ev.implicit.canOmitTagInPlainScalar())
        } else {
            Tag(ev.tag)
        }
        val node = ScalarNode(nodeTag, resolved, ev.value, ev.scalarStyle, ev.startMark, ev.endMark)
        if (anchor != null) {
            registerAnchor(anchor, node)
        }
        return node
    }

    private fun composeSequenceNode(anchor: Anchor?): Node {
        val startEvent = parser.next() as SequenceStartEvent
        val tag = startEvent.tag
        var resolved = false
        val nodeTag = if (tag == null || tag == "!") {
            resolved = true
            Tag.SEQ
        } else {
            Tag(tag)
        }
        val children = mutableListOf<Node>()
        val node = SequenceNode(
            tag = nodeTag,
            resolved = resolved,
            value = children,
            flowStyle = startEvent.flowStyle,
            startMark = startEvent.startMark
        )
        if (anchor != null) {
            registerAnchor(anchor, node)
        }
        while (!parser.checkEvent(Event.ID.SequenceEnd)) {
            children.add(composeNode(node))
        }
        val endEvent = parser.next()
        node.setEndMark(endEvent.endMark)
        return node
    }


    private fun composeMappingNode(anchor: Anchor?): Node {
        val startEvent = parser.next() as MappingStartEvent
        val tag = startEvent.tag

        var resolved = false
        val nodeTag = if (tag == null || tag == "!") {
            resolved = true
            Tag.MAP
        } else {
            Tag(tag)
        }
        val children = mutableListOf<NodeTuple>()
        val node = MappingNode(
            tag = nodeTag,
            resolved = resolved,
            value = children,
            flowStyle = startEvent.flowStyle,
            startMark = startEvent.startMark,
            endMark = null
        )
        if (anchor != null) {
            registerAnchor(anchor, node)
        }
        while (!parser.checkEvent(Event.ID.MappingEnd)) {
            composeMappingChildren(children, node)
        }
        val endEvent = parser.next()
        node.setEndMark(endEvent.endMark)
        return node
    }

    private fun composeMappingChildren(children: MutableList<NodeTuple>, node: MappingNode) {
        val itemKey = composeKeyNode(node)
        val itemValue = composeValueNode(node)
        children.add(NodeTuple(itemKey, itemValue))
    }

    private fun composeKeyNode(node: MappingNode): Node {
        return composeNode(node)
    }

    private fun composeValueNode(node: MappingNode): Node {
        return composeNode(node)
    }
}
