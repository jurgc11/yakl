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
package uk.org.jurg.yakl.engine.v2.common

/**
 * YAML provides a rich set of scalar styles. Block scalar styles include
 * the literal style and the folded style; flow scalar styles include the
 * plain style and two quoted styles, the single-quoted style and the
 * double-quoted style. These styles offer a range of trade-offs between
 * expressive power and readability.
 */
enum class ScalarStyle(private val style: Char?) {
    DOUBLE_QUOTED('"'),
    SINGLE_QUOTED('\''),
    LITERAL('|'),
    FOLDED('>'),
    PLAIN(null);

    override fun toString(): String {
        return style?.toString() ?: ":"
    }
}
