/**
 * Copyright (c) 2018, http://www.snakeyaml.org
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.jurg.yakl.engine.v2.representer

import uk.org.jurg.yakl.engine.v2.api.RepresentToNode
import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.AnchorNode
import uk.org.jurg.yakl.engine.v2.nodes.MappingNode
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.nodes.NodeTuple
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import uk.org.jurg.yakl.engine.v2.nodes.SequenceNode
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import kotlin.reflect.KClass

/**
 * Represent basic YAML structures: scalar, sequence, mapping
 */
abstract class BaseRepresenter {
    /**
     * Keep representers which must match the class exactly
     */
    protected val representers: MutableMap<KClass<*>, RepresentToNode> = HashMap()
    /**
     * in Java 'null' is not a type. So we have to keep the null representer
     * separately otherwise it will coincide with the default representer which
     * is stored with the key null.
     */
    protected var nullRepresenter: RepresentToNode? = null
    // the order is important (map can be also a sequence of key-values)
    /**
     * Keep representers which match a parent of the class to be represented
     */
    protected val parentClassRepresenters: MutableMap<KClass<*>, RepresentToNode> = LinkedHashMap()
    protected var defaultScalarStyle = ScalarStyle.PLAIN
    protected var defaultFlowStyle = FlowStyle.AUTO
    protected val representedObjects: MutableMap<Any, Node> = mutableMapOf()

    fun MutableMap<Any, Node>.putAnchor(key: Any, value: Node) {
        this.put(key, AnchorNode(value))
    }
    protected var objectToRepresent: Any? = null
    /**
     * Represent the provided Java instance to a Node
     *
     * @param data - Java instance to be represented
     * @return The Node to be serialized
     */
    fun represent(data: Any?): Node {
        val node: Node = representData(data)
        representedObjects.clear()
        objectToRepresent = null
        return node
    }

    /**
     * Find the representer which is suitable to represent the internal structure of the provided instance to
     * a Node
     *
     * @param data - the data to be serialized
     * @return RepresentToNode to call to create a Node
     */
    protected open fun findRepresenterFor(data: Any): RepresentToNode? {
        val clazz = data::class
        val representer = representers[clazz]

        // check the same class
        return representer
            ?: // check the parents
            parentClassRepresenters.filter { it.key.isInstance(data) }
                .map { it.value }
                .firstOrNull()
    }

    protected fun representData(data: Any?): Node {
        objectToRepresent = data;
        // check for identity
        representedObjects[objectToRepresent]?.let { return it }
        // check for null first
        if (data == null) {
            return nullRepresenter!!.representData(null)
        }
        val representer = findRepresenterFor(data)
            ?: throw YamlEngineException("Representer is not defined for ${data::class} $data");
        return representer.representData(data)
    }

    protected open fun representScalar(
        tag: Tag,
        value: String,
        style: ScalarStyle = ScalarStyle.PLAIN
    ): Node {
        val defaultedStyle = if (style == ScalarStyle.PLAIN) defaultScalarStyle else  style
        return ScalarNode(
            tag = tag,
            value = value,
            style = defaultedStyle
        )
    }

    protected open fun representSequence(
        tag: Tag,
        sequence: Iterable<*>,
        flowStyle: FlowStyle
    ): Node {
        var size = 10 // default for ArrayList
        if (sequence is List<*>) {
            size = sequence.size
        }
        val value = ArrayList<Node>(size)
        val node = SequenceNode(tag=tag, value = value, flowStyle = flowStyle)
        representedObjects[objectToRepresent!!] = node
        var bestStyle = FlowStyle.FLOW
        sequence.map { representData(it) }.forEach {
            if (!(it is ScalarNode && it.isPlain)) {
                bestStyle = FlowStyle.BLOCK
            }
            value.add(it)
        }

        if (flowStyle === FlowStyle.AUTO) {
            if (defaultFlowStyle != FlowStyle.AUTO) {
                node.flowStyle = defaultFlowStyle
            } else {
                node.flowStyle = bestStyle
            }
        }
        return node
    }

    protected open fun representMapping(
        tag: Tag,
        mapping: Map<*, *>,
        flowStyle: FlowStyle
    ): Node {
        val value = ArrayList<NodeTuple>(mapping.size)
        val node = MappingNode(tag = tag, value = value, flowStyle = flowStyle)
        representedObjects[objectToRepresent!!] = node
        var bestStyle = FlowStyle.FLOW
        mapping.forEach { (key, value1) ->
            val nodeKey = representData(key)
            val nodeValue = representData(value1)
            if (!(nodeKey is ScalarNode && nodeKey.isPlain)) {
                bestStyle = FlowStyle.BLOCK
            }
            if (!(nodeValue is ScalarNode && nodeValue.isPlain)) {
                bestStyle = FlowStyle.BLOCK
            }
            value.add(NodeTuple(nodeKey, nodeValue))
        }
        if (flowStyle === FlowStyle.AUTO) {
            if (defaultFlowStyle !== FlowStyle.AUTO) {
                node.flowStyle = defaultFlowStyle
            } else {
                node.flowStyle = bestStyle
            }
        }
        return node
    }
}
