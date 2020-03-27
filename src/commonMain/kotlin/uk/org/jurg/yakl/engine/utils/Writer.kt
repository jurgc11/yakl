package uk.org.jurg.yakl.engine.utils

interface Writer {
    fun write(str: String)
    fun write(str: String, off: Int, len: Int)

    fun flush()
}
