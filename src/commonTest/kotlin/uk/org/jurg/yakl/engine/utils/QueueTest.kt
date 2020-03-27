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

package uk.org.jurg.yakl.engine.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueTest {

    @Test
    fun x() {
        val queue = Queue<String>(5)
        queue.add("a")
        assertEquals(queue.peek(), "a")
        queue.add("b")
        assertEquals(queue.peek(), "a")
        assertEquals(queue.poll(), "a")
        assertEquals(queue.peek(), "b")
        assertEquals(queue.poll(), "b")
    }

    @Test
    fun iterator() {
        val queue = Queue<String>(5)
        queue.add("a")
        queue.add("b")
        val it = queue.iterator()
        assertTrue(it.hasNext())
        assertEquals(it.next(), "a")
        assertTrue(it.hasNext())
        assertEquals(it.next(), "b")
        assertFalse(it.hasNext())
    }
}
