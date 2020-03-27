package uk.org.jurg.yakl.engine.utils

interface Reader {

    fun read(buffer: CharArray, offset: Int, length: Int): Int

    fun close()
}
