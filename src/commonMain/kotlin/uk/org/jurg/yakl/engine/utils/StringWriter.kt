package uk.org.jurg.yakl.engine.utils

open class StringWriter : Writer {
    val sb = StringBuilder()

    override fun write(str: String) {
        sb.append(str)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun write(str: String, off: Int, len: Int) {
        sb.appendRange(str, off, off+len)
    }

    override fun flush() {
        //No op
    }

    override fun toString(): String {
        return sb.toString()
    }
}
