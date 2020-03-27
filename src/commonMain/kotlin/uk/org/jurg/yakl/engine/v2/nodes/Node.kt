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
package uk.org.jurg.yakl.engine.v2.nodes

import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.exceptions.Mark

/**
 * Base class for all nodes.
 *
 *
 * The nodes form the node-graph described in the [YAML Specification](https://yaml.org/spec/1.2/spec.html).
 *
 *
 *
 * While loading, the node graph is usually created by the
 * [org.snakeyaml.engine.v2.composer.Composer].
 *
 */
abstract class Node(
    /**
     * Tag of this node.
     * <p>
     * Every node has a tag assigned. The tag is either local or global.
     *
     * @return Tag of this node.
     */
    var tag: Tag,

    val startMark: Mark?,
    var endMark: Mark?
) {

    /**
     * Indicates if this node must be constructed in two steps.
     *
     *
     * Two-step construction is required whenever a node is a child (direct or
     * indirect) of it self. That is, if a recursive structure is build using
     * anchors and aliases.
     *
     *
     *
     * Set by [org.snakeyaml.engine.v2.composer.Composer], used during the
     * construction process.
     *
     *
     *
     * Only relevant during loading.
     *
     *
     * @return `true` if the node is self referenced.
     */
    var recursive: Boolean = false

    /**
     * true when the tag is assigned by the resolver
     */
    protected var resolved: Boolean = true

    var anchor: Anchor? = null

    private var properties: MutableMap<String, Any>? = null

    /**
     * @return scalar, sequence, mapping
     */
    abstract val nodeType: NodeType?

    /**
     * Define a custom runtime property. It is not used by Engine but may be used by other tools.
     *
     * @param key   - the key for the custom property
     * @param value - the value for the custom property
     * @return the previous value for the provided key if it was defined
     */
    fun setProperty(key: String, value: Any): Any? {
        if (properties == null) {
            properties = mutableMapOf()
        }
        return properties?.put(key, value)
    }

    /**
     * Get the custom runtime property.
     *
     * @param key - the key of the runtime property
     * @return the value if it was specified
     */
    fun getProperty(key: String): Any? {
        return properties?.get(key)
    }

}
