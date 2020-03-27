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
package uk.org.jurg.yakl.engine.v2.api

import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.Node

/**
 * Provide a way to construct a Java instance from the composed Node. Support
 * recursive objects if it is required. (create Native Data Structure out of
 * Node Graph)
 * (this is the opposite for Represent)
 *
 * @see [Processing Overview](http://www.yaml.org/spec/1.2/spec.html.id2762107)
 */
interface ConstructNode {
    /**
     * Construct a Java instance with all the properties injected when it is possible.
     *
     * @param node composed Node
     * @return a complete Java instance or empty collection instance if it is recursive
     */
    fun construct(node: Node?): Any?

    /**
     * Apply the second step when constructing recursive structures. Because the
     * instance is already created it can assign a reference to itself.
     * (no need to implement this method for non-recursive data structures).
     * Fail with a reminder to provide the seconds step for a recursive
     * structure
     *
     * @param node   composed Node
     * @param `object` the instance constructed earlier by
     * `construct(Node node)` for the provided Node
     */
    fun constructRecursive(node: Node, value: Any?) {
        if (node.recursive) {
            throw IllegalStateException("Not implemented in " + this::class.qualifiedName)
        } else {
            throw YamlEngineException("Unexpected recursive structure for Node: $node")
        }
    }
}
