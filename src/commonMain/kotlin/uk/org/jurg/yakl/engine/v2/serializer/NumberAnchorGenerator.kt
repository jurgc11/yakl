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
package uk.org.jurg.yakl.engine.v2.serializer

import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.nodes.Node

class NumberAnchorGenerator(private var lastAnchorId: Int = 0) : AnchorGenerator {

    override fun nextAnchor(node: Node?): Anchor? {
        lastAnchorId++
        val anchorId = when(lastAnchorId) {
            in 0..9     -> "id00$lastAnchorId"
            in 10..99   -> "id0$lastAnchorId"
            else        -> "id$lastAnchorId"
        }
        return Anchor(anchorId)
    }
}
