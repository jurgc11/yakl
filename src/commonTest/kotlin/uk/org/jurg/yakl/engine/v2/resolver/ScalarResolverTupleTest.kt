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
package uk.org.jurg.yakl.engine.v2.resolver

import uk.org.jurg.yakl.engine.v2.nodes.Tag
import kotlin.test.Test
import kotlin.test.assertEquals

//@org.junit.jupiter.api.Tag("fast")
 class ScalarResolverTupleTest {
    @Test
    fun `ResolverTuple.toString()`() {
        assertEquals(
            "Tuple tag=tag:yaml.org,2002:str regexp=^(?:true|false)$",
            ResolverTuple(Tag.STR, "^(?:true|false)$".toRegex()).toString()
        )
    }
}
