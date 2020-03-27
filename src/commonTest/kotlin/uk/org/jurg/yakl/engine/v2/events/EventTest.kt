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
package uk.org.jurg.yakl.engine.v2.events

import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.exceptions.Mark
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class EventTest {
    @Test
    fun testToString() {
        val alias: Event =
            AliasEvent(Anchor("111"))
        assertFalse(alias.equals(alias.toString()))
    }

    @Test
    fun bothMarks() {
        val fake = Mark("a", 0, 0, 0, IntArray(0), 0)
        assertFailsWith(NullPointerException::class, "Both marks must be either present or absent.") {
            StreamStartEvent(null, fake)
        }
    }
}