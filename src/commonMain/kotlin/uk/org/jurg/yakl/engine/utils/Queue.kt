package uk.org.jurg.yakl.engine.utils

//TODO can this be replaced by an ArrayDeque?
class Queue<T>(private val maxSize: Int) : Iterable<T> {
    @Suppress("UNCHECKED_CAST")
    private val array = arrayOfNulls<Any?>(maxSize) as Array<T?>

    private var head = 0
    private var tail = 0
    var size = 0
        private set

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    fun peek(): T {
        if (size == 0) throw UnderflowException("Queue is empty, can't dequeue()")
        return array[head]!!
    }

    fun add(item: T) {
        // Check if there's space before attempting to add the item
        if (size == maxSize) throw OverflowException("Can't add $item, queue is full")

        array[tail] = item
        // Loop around to the start of the array if there's a need for it
        tail = (tail + 1) % maxSize
        size++
    }

    fun poll(): T {
        // Check if queue is empty before attempting to remove the item
        if (size == 0) throw UnderflowException("Queue is empty, can't dequeue()")

        val result = array[head]!!
        // Loop around to the start of the array if there's a need for it
        head = (head + 1) % maxSize
        size--

        return result
    }

    override fun iterator(): QueueIterator {
        return QueueIterator()
    }

    inner class QueueIterator: Iterator<T> {
        private var pointer = head
        private var count = size

        override fun hasNext(): Boolean {
            return count > 0
        }

        override fun next(): T {
            val result = array[pointer]!!
            pointer += 1 % maxSize
            count--
            return result
        }

    }
}

class OverflowException(msg: String) : RuntimeException(msg)
class UnderflowException(msg: String) : RuntimeException(msg)
