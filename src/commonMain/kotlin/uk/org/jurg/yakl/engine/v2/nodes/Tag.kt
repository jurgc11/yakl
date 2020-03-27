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

package uk.org.jurg.yakl.engine.v2.nodes

import uk.org.jurg.yakl.engine.utils.UriEncoder
import kotlin.reflect.KClass

class Tag {
    val value: String

    constructor(tag: String) {
        require(tag.isNotEmpty()) { "Tag must not be empty." }
        require(tag.trim().length == tag.length) { "Tag must not contain leading or trailing spaces." }
        value = UriEncoder.encode(tag)
    }

    /**
     * Create a global tag to dump the fully qualified class name
     *
     * @param clazz - the class to use the name
     */
    constructor(clazz: KClass<out Any>) {
        value = PREFIX + UriEncoder.encode(clazz.qualifiedName!!)
    }

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Tag) {
            value == other.value
        } else false
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    companion object {
        const val PREFIX = "tag:yaml.org,2002:"
        val SET = Tag(PREFIX + "set")
        val BINARY = Tag(PREFIX + "binary")
        val INT = Tag(PREFIX + "int")
        val FLOAT = Tag(PREFIX + "float")
        val BOOL = Tag(PREFIX + "bool")
        val NULL = Tag(PREFIX + "null")
        val STR = Tag(PREFIX + "str")
        val SEQ = Tag(PREFIX + "seq")
        val MAP = Tag(PREFIX + "map")
        val ENV_TAG = Tag("!ENV_VARIABLE")
    }
}
