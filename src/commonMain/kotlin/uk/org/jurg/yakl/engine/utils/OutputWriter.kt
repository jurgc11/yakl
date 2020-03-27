package uk.org.jurg.yakl.engine.utils

import kotlinx.io.Output
import kotlinx.io.text.Charset
import kotlinx.io.text.name
import kotlinx.io.text.writeUtf8String

//TODO this is rubbish
class OutputWriter(private val output: Output, private val charset: Charset) : Writer {

    init {
        if (charset.name != "UTF-8") {
            throw UnsupportedOperationException("Unsupported character set: ${charset.name}")
        }
    }

    override fun write(str: String) {
        output.writeUtf8String(str)
    }

    override fun write(str: String, off: Int, len: Int) {
        output.writeUtf8String(str.subSequence(off, off+len))
    }

    override fun flush() {
        output.flush()
    }
}
