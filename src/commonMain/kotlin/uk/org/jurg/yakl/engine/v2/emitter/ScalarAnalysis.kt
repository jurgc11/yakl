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
package uk.org.jurg.yakl.engine.v2.emitter

/**
 * Accumulate information to choose the scalar style
 */
data class ScalarAnalysis(
    val scalar: String,
    val empty: Boolean,
    val multiline: Boolean,
    val allowFlowPlain: Boolean,
    val allowBlockPlain: Boolean,
    val allowSingleQuoted: Boolean,
    val allowBlock: Boolean
)
