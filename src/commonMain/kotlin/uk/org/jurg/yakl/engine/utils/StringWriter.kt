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

class StringWriter : Writer {
    private val sb = StringBuilder()

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
