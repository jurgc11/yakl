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
