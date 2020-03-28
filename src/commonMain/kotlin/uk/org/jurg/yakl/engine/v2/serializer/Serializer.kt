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
package uk.org.jurg.yakl.engine.v2.serializer

import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.emitter.Emitable
import uk.org.jurg.yakl.engine.v2.events.AliasEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentEndEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentStartEvent
import uk.org.jurg.yakl.engine.v2.events.ImplicitTuple
import uk.org.jurg.yakl.engine.v2.events.MappingEndEvent
import uk.org.jurg.yakl.engine.v2.events.MappingStartEvent
import uk.org.jurg.yakl.engine.v2.events.ScalarEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceEndEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceStartEvent
import uk.org.jurg.yakl.engine.v2.events.StreamEndEvent
import uk.org.jurg.yakl.engine.v2.events.StreamStartEvent
import uk.org.jurg.yakl.engine.v2.nodes.AnchorNode
import uk.org.jurg.yakl.engine.v2.nodes.CollectionNode
import uk.org.jurg.yakl.engine.v2.nodes.MappingNode
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.nodes.NodeType
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import uk.org.jurg.yakl.engine.v2.nodes.SequenceNode
import uk.org.jurg.yakl.engine.v2.nodes.Tag

class Serializer(private val settings: DumpSettings,
                 private val emitable: Emitable
) {
    private var serializedNodes: MutableSet<Node> = mutableSetOf()
    private var anchors: MutableMap<Node, Anchor?> = mutableMapOf()

    fun serialize(node: Node) {
        this.emitable.emit(DocumentStartEvent(settings.explicitStart, settings.yamlDirective, settings.tagDirective))
        anchorNode(node)
        settings.explicitRootTag?.let { node.tag = it }
        serializeNode(node)
        this.emitable.emit(DocumentEndEvent(settings.explicitEnd))
        this.serializedNodes.clear()
        this.anchors.clear()
    }

    fun open() {
        this.emitable.emit(StreamStartEvent())
    }

    fun close() {
        emitable.emit(StreamEndEvent())
        // clean up the resources
        anchors.clear()
        serializedNodes.clear()
    }

    private fun anchorNode(node: Node) {
        val realNode = if (node.nodeType === NodeType.ANCHOR) {
            (node as AnchorNode).realNode
        } else {
            node
        }

        if (anchors.containsKey(realNode)) {
            // this looks weird, anchors does contain the key node but we call computeIfAbsent()
            // this is because the value is null (HashMap permits values to be null)
            val value = anchors[realNode]
            if (value == null) {
                anchors[realNode] = settings.anchorGenerator.nextAnchor(
                    realNode
                )
            }
        } else {
            anchors[realNode] = null
            when (realNode.nodeType) {
                NodeType.SEQUENCE -> {
                    val seqNode = realNode as SequenceNode
                    seqNode.value.forEach {
                        anchorNode(it)
                    }
                }
                NodeType.MAPPING -> {
                    val mappingNode = realNode as MappingNode
                    mappingNode.value.forEach {
                        anchorNode(it.keyNode)
                        anchorNode(it.valueNode)
                    }
                }
                else -> {}
            }
        }
    }

    private fun serializeNode(node2: Node) {
        val node = if (node2.nodeType == NodeType.ANCHOR) {
            (node2 as AnchorNode).realNode
        } else {
            node2
        }
        val tAlias = anchors[node]
        if (serializedNodes.contains(node)) {
            emitable.emit(AliasEvent(tAlias))
        } else {
            serializedNodes.add(node)
            when (node.nodeType) {
                NodeType.SCALAR -> {
                    val scalarNode = node as ScalarNode
                    val detectedTag =
                        settings.scalarResolver.resolve(scalarNode.value, true)
                    val defaultTag =
                        settings.scalarResolver.resolve(scalarNode.value, false)
                    val tuple = ImplicitTuple(
                        node.tag == detectedTag,
                        node.tag == defaultTag
                    )
                    val event = ScalarEvent(
                        anchor = tAlias,
                        tag = node.tag.value,
                        implicit = tuple,
                        value = scalarNode.value,
                        scalarStyle = scalarNode.style
                    )
                    emitable.emit(event)
                }
                NodeType.SEQUENCE -> {
                    val seqNode = node as SequenceNode
                    val implicitS = node.tag == Tag.SEQ
                    emitable.emit(
                        SequenceStartEvent(
                            tAlias,
                            node.tag.value,
                            implicitS,
                            seqNode.flowStyle
                        )
                    )
                    val list = seqNode.value
                    for (item in list) {
                        serializeNode(item)
                    }
                    emitable.emit(SequenceEndEvent())
                }
                else -> {
                    val implicitM = node.tag.equals(Tag.MAP)
                    emitable.emit(
                        MappingStartEvent(
                            tAlias, node.tag.value,
                            implicitM, (node as CollectionNode<*>).flowStyle
                        )
                    )
                    val mappingNode = node as MappingNode
                    val map = mappingNode.value
                    for (entry in map) {
                        val key = entry.keyNode
                        val value = entry.valueNode
                        serializeNode(key)
                        serializeNode(value)
                    }
                    emitable.emit(MappingEndEvent())
                }
            }
        }
    }
}
