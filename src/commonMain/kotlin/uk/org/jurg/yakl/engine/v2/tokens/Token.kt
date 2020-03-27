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


package uk.org.jurg.yakl.engine.v2.tokens

import uk.org.jurg.yakl.engine.v2.exceptions.Mark

abstract class Token(
    val startMark: Mark?,
    val endMark: Mark?
) {
    enum class ID(private val description: String) {
        Alias("<alias>"),  //NOSONAR
        Anchor("<anchor>"),  //NOSONAR
        BlockEnd("<block end>"),  //NOSONAR
        BlockEntry("-"),  //NOSONAR
        BlockMappingStart("<block mapping start>"),  //NOSONAR
        BlockSequenceStart("<block sequence start>"),  //NOSONAR
        Directive("<directive>"),  //NOSONAR
        DocumentEnd("<document end>"),  //NOSONAR
        DocumentStart("<document start>"),  //NOSONAR
        FlowEntry(","),  //NOSONAR
        FlowMappingEnd("}"),  //NOSONAR
        FlowMappingStart("{"),  //NOSONAR
        FlowSequenceEnd("]"),  //NOSONAR
        FlowSequenceStart("["),  //NOSONAR
        Key("?"),  //NOSONAR
        Scalar("<scalar>"),  //NOSONAR
        StreamEnd("<stream end>"),  //NOSONAR
        StreamStart("<stream start>"),  //NOSONAR
        Tag("<tag>"),  //NOSONAR
        Value(":");

        override fun toString(): String {
            return description
        }
    }

    /**
     * For error reporting.
     *
     * @return ID of this token
     */
    abstract val tokenId: ID

    override fun toString(): String {
        return tokenId.toString()
    }
}
