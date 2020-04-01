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
package uk.org.jurg.yakl.engine.v2.constructor

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuidFrom
import uk.org.jurg.yakl.engine.utils.EnvironmentVariables
import uk.org.jurg.yakl.engine.utils.base64Decode
import uk.org.jurg.yakl.engine.v2.api.ConstructNode
import uk.org.jurg.yakl.engine.v2.api.LoadSettings
import uk.org.jurg.yakl.engine.v2.exceptions.ConstructorException
import uk.org.jurg.yakl.engine.v2.exceptions.DuplicateKeyException
import uk.org.jurg.yakl.engine.v2.exceptions.Mark
import uk.org.jurg.yakl.engine.v2.exceptions.MissingEnvironmentVariableException
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.MappingNode
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.nodes.NodeTuple
import uk.org.jurg.yakl.engine.v2.nodes.ScalarNode
import uk.org.jurg.yakl.engine.v2.nodes.SequenceNode
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.resolver.ENV_FORMAT
import uk.org.jurg.yakl.engine.v2.resolver.NAME_GROUP
import uk.org.jurg.yakl.engine.v2.resolver.SEPARATOR_GROUP
import uk.org.jurg.yakl.engine.v2.resolver.VALUE_GROUP

/**
 * Construct standard Java classes
 */
open class StandardConstructor(settings: LoadSettings) : BaseConstructor(settings) {

    init {
        tagConstructors[Tag.NULL] = ConstructYamlNull()
        tagConstructors[Tag.BOOL] = ConstructYamlBool()
        tagConstructors[Tag.INT] = ConstructYamlInt()
        tagConstructors[Tag.FLOAT] = ConstructYamlFloat()
        tagConstructors[Tag.BINARY] = ConstructYamlBinary()
        tagConstructors[Tag.SET] = ConstructYamlSet()
        tagConstructors[Tag.STR] = ConstructYamlStr()
        tagConstructors[Tag.SEQ] = ConstructYamlSeq()
        tagConstructors[Tag.MAP] = ConstructYamlMap()
        tagConstructors[Tag.ENV_TAG] = ConstructEnv()
        tagConstructors[Tag(Uuid::class)] = ConstructUuidClass()
        tagConstructors.putAll(settings.tagConstructors)
    }

    protected fun flattenMapping(node: MappingNode) {
        // perform merging only on nodes containing merge node(s)
        processDuplicateKeys(node)
        if (node.merged) {
            node.value = mergeNode(node, true, HashMap(), ArrayList())
        }
    }

    protected fun processDuplicateKeys(node: MappingNode) {
        val nodeValue = node.value
        val keys = HashMap<Any?, Int>(nodeValue.size)

        val toRemove: MutableSet<Int> = mutableSetOf()
        var i = 0
        nodeValue.forEach { tuple ->
            val keyNode = tuple.keyNode
            val key = constructKey(keyNode, node.startMark, keyNode.startMark)
            val prevIndex = keys.put(key, i)
            if (prevIndex != null) {
                if (!settings.allowDuplicateKeys) {
                    throw DuplicateKeyException(node.startMark, key, keyNode.startMark)
                }
                toRemove.add(prevIndex)
            }
            i += 1
        }
        toRemove.sortedDescending().forEach {
            nodeValue.removeAt(it)
        }
    }

    private fun constructKey(
        keyNode: Node,
        contextMark: Mark?,
        problemMark: Mark?
    ): Any? { //CHECKED definitely nullable
        val key = constructObject(keyNode)
        if (key != null) {
            try {
                key.hashCode() // check circular dependencies
            } catch (e: Exception) {
                throw ConstructorException(
                    context = "while constructing a mapping",
                    contextMark = contextMark,
                    problem = "found unacceptable key $key",
                    problemMark = problemMark,
                    cause = e
                )
            }
        }
        return key
    }

    /**
     * Does merge for supplied mapping node.
     *
     * @param node        where to merge
     * @param isPreferred true if keys of node should take precedence over others...
     * @param key2index   maps already merged keys to index from values
     * @param values      collects merged NodeTuple
     * @return list of the merged NodeTuple (to be set as value for the MappingNode)
     */
    private fun mergeNode(
        node: MappingNode,
        isPreferred: Boolean,
        key2index: MutableMap<Any?, Int>,
        values: MutableList<NodeTuple>
    ): MutableList<NodeTuple> {

        node.value.forEach {
            val nodeTuple: NodeTuple = it
            val keyNode = nodeTuple.keyNode
            // we need to construct keys to avoid duplications
            val key = constructObject(keyNode)
            if (!key2index.containsKey(key)) { // 1st time merging key
                values.add(nodeTuple)
                // keep track where tuple for the key is
                key2index[key] = values.size - 1
            } else if (isPreferred) {
                // there is value for the key, but we need to override it
                // change value for the key using saved position
                values[key2index[key]!!] = nodeTuple
            }
        }
        return values
    }


    override fun constructMapping2ndStep(node: MappingNode, mapping: MutableMap<Any?, Any?>) {
        flattenMapping(node)
        super.constructMapping2ndStep(node, mapping)
    }

    override fun constructSet2ndStep(node: MappingNode, set: MutableSet<Any?>) {
        flattenMapping(node)
        super.constructSet2ndStep(node, set)
    }

    inner class ConstructYamlNull : ConstructNode {
        override fun construct(node: Node?): Any? {
            if (node != null) constructScalar(node as ScalarNode)
            return null
        }
    }

    inner class ConstructYamlBool : ConstructNode {
        override fun construct(node: Node?): Any? {
            return constructScalar(node as ScalarNode).toBoolean()
        }
    }

    inner class ConstructYamlInt : ConstructNode {

        override fun construct(node: Node?): Any? {
            val value = constructScalar(node as ScalarNode)
            return createIntNumber(value)
        }

        protected fun createIntNumber(number: String): Number {
            try {
                return try { //first try integer
                    number.toInt()
                } catch (e: NumberFormatException) {
                    number.toLong()
                }
            } catch (e: NumberFormatException) {
                throw NumberFormatException("""For input string: "$number"""")
            }
        }
    }

    inner class ConstructYamlFloat : ConstructNode {
        override fun construct(node: Node?): Any? {
            val value: String = constructScalar(node as ScalarNode)
            return value.toDouble()
        }
    }

    inner class ConstructYamlBinary : ConstructNode {
        private val whitespace = "\\s".toRegex()
        override fun construct(node: Node?): Any? {
            // Ignore white spaces for base64 encoded scalar
            return constructScalar(node as ScalarNode)
                .replace(whitespace, "")
                .base64Decode()
        }
    }

    inner class ConstructUuidClass : ConstructNode {
        override fun construct(node: Node?): Any? {
            val uuidValue: String = constructScalar(node as ScalarNode)
            return uuidFrom(uuidValue)
        }
    }

    inner class ConstructYamlSet : ConstructNode {

        override fun construct(node: Node?): Any? {
            return if (node!!.recursive) {
                return constructedObjects[node]?: createDefaultSet((node as MappingNode).value.size)
            } else {
                constructSet(node as MappingNode)
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun constructRecursive(node: Node, value: Any?) {
            if (node.recursive) {
                constructSet2ndStep(node as MappingNode, value as MutableSet<Any?>)
            } else {
                throw YamlEngineException("Unexpected recursive set structure. Node: $node")
            }
        }
    }

    inner class ConstructYamlStr : ConstructNode {

        override fun construct(node: Node?): Any? {
            return constructScalar(node as ScalarNode)
        }
    }

    inner class ConstructYamlSeq : ConstructNode {

        override fun construct(node: Node?): Any? {
            val seqNode = node as SequenceNode
            return if (seqNode.recursive) {
                settings.defaultList.invoke(seqNode.value.size)
            } else {
                constructSequence(seqNode)
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun constructRecursive(node: Node, value: Any?) {
            if (node.recursive) {
                constructSequenceStep2(node as SequenceNode, value as MutableList<Any?>)
            } else {
                throw YamlEngineException("Unexpected recursive sequence structure. Node: $node")
            }
        }
    }

    inner class ConstructYamlMap : ConstructNode {

        override fun construct(node: Node?): Any? {
            val mappingNode = node as MappingNode
            return if (mappingNode.recursive) {
                createDefaultMap(mappingNode.value.size)
            } else {
                constructMapping(mappingNode)
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun constructRecursive(node: Node, value: Any?) {
            if (node.recursive) {
                constructMapping2ndStep(node as MappingNode, value as MutableMap<Any?, Any?>)
            } else {
                throw YamlEngineException("Unexpected recursive mapping structure. Node: $node")
            }
        }
    }

    /**
     * Construct scalar for format ${VARIABLE} replacing the template with the value from environment.
     *
     * @see [Variable substitution](https://bitbucket.org/asomov/snakeyaml/wiki/Variable%20substitution)
     * @see [Variable substitution](https://docs.docker.com/compose/compose-file/.variable-substitution)
     */
    inner class ConstructEnv : ConstructNode {
        override fun construct(node: Node?): Any? {
            val scalar = constructScalar(node as ScalarNode)
            val config = settings.envConfig
            return if (config != null) {

                val matcher = ENV_FORMAT.matchEntire(scalar)!!.groups

                val name = matcher[NAME_GROUP]!!.value
                val value = matcher[VALUE_GROUP]?.value ?: ""
                val separator = matcher[SEPARATOR_GROUP]?.value

                val env = getEnv(name)
                val overruled = config.getValueFor(name, separator, value, env)
                overruled ?: apply(name, separator, value, env)
            } else {
                scalar
            }
        }

        /**
         * Implement the logic for missing and unset variables
         *
         * @param name        - variable name in the template
         * @param separator   - separator in the template, can be :-, -, :?, ?
         * @param value       - default value or the error in the template
         * @param environment - the value from environment for the provided variable
         * @return the value to apply in the template
         */
        fun apply(name: String, separator: String?, value: String, environment: String?): String {
            if (environment != null && environment.isNotEmpty()) return environment
            // variable is either unset or empty

            if (separator != null) {
                //there is a default value or error
                when {
                    separator == "?" -> {
                        if (environment == null) {
                            throw MissingEnvironmentVariableException("Missing mandatory variable $name: $value")
                        }
                    }
                    separator == ":?" -> {
                        if (environment == null) {
                            throw MissingEnvironmentVariableException("Missing mandatory variable $name: $value")
                        }
                        if (environment.isEmpty()) {
                            throw MissingEnvironmentVariableException("Empty mandatory variable $name: $value")
                        }
                    }
                    separator.startsWith(":") -> {
                        if (environment == null || environment.isEmpty()) {
                            return value
                        }
                    }
                    else -> {
                        if (environment == null) {
                            return value
                        }
                    }
                }
            }
            return ""
        }

        /**
         * Get value of the environment variable
         *
         * @param key - the name of the variable
         * @return value or null if not set
         */
        fun getEnv(key: String): String? {
            return EnvironmentVariables.get(key)
        }
    }


}
