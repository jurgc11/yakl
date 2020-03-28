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
