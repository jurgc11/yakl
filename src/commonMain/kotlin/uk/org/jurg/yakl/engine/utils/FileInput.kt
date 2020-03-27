package uk.org.jurg.yakl.engine.utils

import kotlinx.io.Input
import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.storeByteArray

class FileInput(path: String) : Input() {

    private var startIndex = 0
    private var endIndex = -1
    private var currentIndex = startIndex
    private val source: ByteArray

    init {
        //TODO yeah...
        @OptIn(ExperimentalStdlibApi::class)
        source = Files.readFile(path).encodeToByteArray()
        endIndex = source.size
    }

    override fun closeSource() {
        //No-op
    }

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        val size = (this.endIndex - currentIndex).coerceAtMost(endIndex - startIndex)
        buffer.storeByteArray(startIndex, source, currentIndex, size)
        currentIndex += size
        return size
    }

}
