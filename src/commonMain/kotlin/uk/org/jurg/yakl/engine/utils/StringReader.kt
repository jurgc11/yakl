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

import kotlin.math.min

class StringReader(private val str: String) : Reader {

    private val length = str.length
    private var next = 0
    private val mark = 0

    override fun read(buffer: CharArray, offset: Int, length: Int): Int {
        if (offset < 0 || offset > buffer.size || length < 0 ||
            offset + length > buffer.size || offset + length < 0
        ) {
            throw IndexOutOfBoundsException()
        } else if (length == 0) {
            return 0
        }
        if (next >= this.length) return -1
        val n = min(this.length - next, length)

        copySubstring(str,next, next + n, buffer, offset)
        next += n
        return n
    }

    private fun copySubstring(src: String, srcBegin: Int, srcEnd: Int, buffer: CharArray, offset: Int) {
        for (i in 0 until srcEnd-srcBegin) {
            buffer[i+offset] = src[i+srcBegin]
        }
    }

    override fun close() {
        // No-op
    }
}
