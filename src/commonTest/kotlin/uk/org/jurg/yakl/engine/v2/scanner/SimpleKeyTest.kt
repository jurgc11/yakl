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
package uk.org.jurg.yakl.engine.v2.scanner

import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleKeyTest {
    @Test
    fun `Resolve implicit integer`() {
        val simpleKey = SimpleKey(0, true, 0, 0, 0, null)
        assertEquals("SimpleKey - tokenNumber=0 required=true index=0 line=0 column=0", simpleKey.toString())
    }
}
