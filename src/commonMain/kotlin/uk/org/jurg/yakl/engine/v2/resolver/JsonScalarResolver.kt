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

package uk.org.jurg.yakl.engine.v2.resolver

import uk.org.jurg.yakl.engine.v2.nodes.Tag

private val BOOL: Regex = "^(?:true|false)$".toRegex()
private val FLOAT: Regex = "^(-?(0?\\.[0-9]+|[1-9][0-9]*(\\.[0-9]*)?)([eE][-+]?[0-9]+)?)$".toRegex()
private val INT: Regex = "^(?:-?(?:0|[1-9][0-9]*))$".toRegex()
private val NULL: Regex = "^(?:null)$".toRegex()
private val EMPTY: Regex = "^$".toRegex()

const val NAME_GROUP = 2
const val SEPARATOR_GROUP = 4
const val VALUE_GROUP = 6
val ENV_FORMAT = Regex("""^\$\{\s*((\w+)((:?([-?]))(\w+)?)?)\s*}$""")

open class JsonScalarResolver : ScalarResolver {

    protected val yamlImplicitResolvers = HashMap<Char?, MutableList<ResolverTuple>>()

    init {
        addImplicitResolvers()
    }

    open fun addImplicitResolver(
        tag: Tag,
        regexp: Regex,
        first: String?
    ) {
        if (first == null) {
            val curr = yamlImplicitResolvers.getOrPut(null) { mutableListOf() }
            curr.add(ResolverTuple(tag, regexp))
        } else {
            first.forEach {
                val theC = if (it == 0.toChar()) null else it
                val curr = yamlImplicitResolvers.getOrPut(theC) { mutableListOf() }
                curr.add(ResolverTuple(tag, regexp))
            }
        }
    }

    protected fun addImplicitResolvers() {
        addImplicitResolver(Tag.NULL, EMPTY, null)
        addImplicitResolver(Tag.BOOL, BOOL, "tf")
        /*
         * INT must be before FLOAT because the regular expression for FLOAT
         * matches INT (see issue 130)
         * http://code.google.com/p/snakeyaml/issues/detail?id=130
         */
        addImplicitResolver(Tag.INT, INT, "-0123456789")
        addImplicitResolver(Tag.FLOAT, FLOAT, "-0123456789.")
        addImplicitResolver(Tag.NULL, NULL, "n\u0000")
        addImplicitResolver(Tag.ENV_TAG, ENV_FORMAT, "$")
    }

    override fun resolve(value: String, implicit: Boolean): Tag {
        if (!implicit) {
            return Tag.STR
        }
        val resolvers = if (value.isEmpty()) {
            yamlImplicitResolvers['\u0000']
        } else {
            yamlImplicitResolvers[value[0]]
        }
        resolvers?.filter { it.regexp.matches(value) }?.forEach {
            return it.tag
        }
        yamlImplicitResolvers[null]?.filter { it.regexp.matches(value) }?.forEach {
            return it.tag
        }
        return Tag.STR
    }
}
