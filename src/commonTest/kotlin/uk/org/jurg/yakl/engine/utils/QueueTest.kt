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
