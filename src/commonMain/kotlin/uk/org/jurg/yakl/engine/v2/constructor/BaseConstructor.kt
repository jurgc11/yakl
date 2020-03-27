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

package uk.org.jurg.yakl.engine.v2.constructor

import uk.org.jurg.yakl.engine.v2.api.ConstructNode
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.exceptions.ConstructorException
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.MappingNode
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import uk.org.jurg.yakl.engine.v2.nodes.SequenceNode
import uk.org.jurg.yakl.engine.v2.nodes.Tag

open class BaseConstructor(protected val settings: LoadSettings) {
    /**
     * It maps the (explicit or implicit) tag to the Construct implementation.
     */
    protected val tagConstructors: MutableMap<Tag, ConstructNode> = mutableMapOf()
    val constructedObjects: MutableMap<Node, Any?> = mutableMapOf()
    private val recursiveObjects: MutableSet<Node> = mutableSetOf()
    private val maps2fill: MutableList<Pair<MutableMap<Any?, Any?>, Pair<Any?, Any?>>> = mutableListOf()
    private val sets2fill: MutableList<Pair<MutableSet<Any?>, Any?>> = mutableListOf()

    /**
     * Ensure that the stream contains a single document and construct it
     *
     * @param optionalNode - composed Node
     * @return constructed instance
     */
    open fun constructSingleDocument(optionalNode: Node?): Any? {
        return if (optionalNode == null || optionalNode.tag == Tag.NULL) {
            tagConstructors.getValue(Tag.NULL).construct(optionalNode)
        } else {
            construct(optionalNode)
        }
    }

    /**
     * Construct complete YAML document. Call the second step in case of
     * recursive structures. At the end cleans all the state.
     *
     * @param node root Node
     * @return Java instance
     */
    open fun construct(node: Node): Any? {
        return try {
            val data = constructObject(node)
            fillRecursive()
            data
        } catch (e: YamlEngineException) {
            throw e
        } catch (e: RuntimeException) {
            throw YamlEngineException(e)
        } finally {
            constructedObjects.clear()
            recursiveObjects.clear()
        }
    }

    private fun fillRecursive() {
        if (maps2fill.isNotEmpty()) {
            for (entry in maps2fill) {
                val keyValueTuple = entry.second
                entry.first[keyValueTuple.first] = keyValueTuple.second
            }
            maps2fill.clear()
        }
        if (sets2fill.isNotEmpty()) {
            for (value in sets2fill) {
                value.first.add(value.second)
            }
            sets2fill.clear()
        }
    }

    /**
     * Construct object from the specified Node. Return existing instance if the
     * node is already constructed.
     *
     * @param node Node to be constructed
     * @return Java instance
     */
    protected open fun constructObject(node: Node): Any? {
        return constructedObjects[node] ?: constructObjectNoCheck(node)
    }

    protected open fun constructObjectNoCheck(node: Node): Any? {
        if (recursiveObjects.contains(node)) {
            throw ConstructorException(problem = "found unconstructable recursive node", problemMark = node.startMark)
        }
        recursiveObjects.add(node)
        val constructor = findConstructorFor(node) ?: throw ConstructorException(
            problem = "could not determine a constructor for the tag " + node.tag, problemMark = node.startMark)
        val data = constructedObjects[node] ?: constructor.construct(node)

        constructedObjects[node] = data
        recursiveObjects.remove(node)
        if (node.recursive) {
            constructor.constructRecursive(node, data)
        }
        return data
    }

    /**
     * Select {@link ConstructNode} inside the provided {@link Node} or the one associated with the {@link Tag}
     *
     * @param node {@link Node} to construct an instance from
     * @return {@link ConstructNode} implementation for the specified node
     */
    protected open fun findConstructorFor(node: Node): ConstructNode? {
        val tag = node.tag
        return settings.tagConstructors[tag] ?: tagConstructors[tag]
    }

    protected open fun constructScalar(node: ScalarNode): String {
        return node.value
    }

    protected open fun createDefaultList(initSize: Int): List<Any?> {
        return ArrayList(initSize)
    }
    protected open fun createDefaultSet(initSize: Int): Set<Any?> {
        return LinkedHashSet(initSize)
    }

    protected open fun createDefaultMap(initSize: Int): Map<Any?, Any?> {
        // respect order from YAML document
        return LinkedHashMap(initSize)
    }

//    protected open fun createArray(type: KClass<*>, size: Int): Any? {
//        return type.constructors.first().call(size)
//        //return Array.newInstance(type.getComponentType(), size)
//    }

    // <<<< DEFAULTS <<<<
    // <<<< NEW instance
    protected open fun constructSequence(node: SequenceNode): List<Any?> {
        val result = settings.defaultList.invoke(node.value.size)
        constructSequenceStep2(node, result)
        return result
    }

    protected open fun constructSequenceStep2(node: SequenceNode, collection: MutableCollection<Any?>) {
        for (child in node.value) {
            collection.add(constructObject(child))
        }
    }

    protected open fun constructSet(node: MappingNode): Set<Any?> {
        val set = settings.defaultSet.invoke(node.value.size)
        constructSet2ndStep(node, set)
        return set
    }

    protected open fun constructMapping(node: MappingNode): Map<Any?, Any?> {
        val mapping = settings.defaultMap.invoke(node.value.size)
        constructMapping2ndStep(node, mapping)
        return mapping
    }

    protected open fun constructSet2ndStep(node: MappingNode, set: MutableSet<Any?>) {
        node.value.forEach {
            val keyNode = it.keyNode
            val key = constructObject(keyNode)
            if (key != null) {
                try {
                    key.hashCode() // check circular dependencies
                } catch (e: Exception) {
                    throw ConstructorException(
                        "while constructing a Set", node.startMark,
                        "found unacceptable key $key", it.keyNode.startMark, e
                    )
                }
            }
            if (keyNode.recursive) {
                if (settings.allowRecursiveKeys) {
                    postponeSetFilling(set, key)
                } else {
                    throw YamlEngineException("Recursive key for mapping is detected but it is not configured to be allowed.")
                }
            } else {
                set.add(key)
            }
        }
    }

    /*
     * if keyObject is created it 2 steps we should postpone putting
     * it into the set because it may have different hash after
     * initialization compared to clean just created one. And set of
     * course does not observe value hashCode changes.
     */
    protected open fun postponeSetFilling(set: MutableSet<Any?>, key: Any?) {
        sets2fill.add(0, Pair(set, key))
    }

    protected open fun constructMapping2ndStep(node: MappingNode, mapping: MutableMap<Any?, Any?>) {
        node.value.forEach {
            val keyNode = it.keyNode
            val valueNode = it.valueNode
            val key = constructObject(keyNode)
            if (key != null) {
                try {
                    key.hashCode() // check circular dependencies
                } catch (e: Exception) {
                    throw ConstructorException(
                        "while constructing a mapping",
                        node.startMark, "found unacceptable key $key",
                        it.keyNode.startMark, e
                    )
                }
            }
            val value = constructObject(valueNode)
            if (keyNode.recursive) {
                if (settings.allowRecursiveKeys) {
                    postponeMapFilling(mapping, key, value)
                } else {
                    throw YamlEngineException("Recursive key for mapping is detected but it is not configured to be allowed.")
                }
            } else {
                mapping[key] = value
            }
        }
    }

    /*
     * if keyObject is created it 2 steps we should postpone putting
     * it in map because it may have different hash after
     * initialization compared to clean just created one. And map of
     * course does not observe key hashCode changes.
     */
    protected open fun postponeMapFilling(mapping: MutableMap<Any?, Any?>, key: Any?, value: Any?) {
        maps2fill.add(
            0,
            Pair(mapping, Pair(key, value))
        )
    }
}

