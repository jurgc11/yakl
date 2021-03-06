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

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.opendir
import platform.posix.readdir

actual object Files {
    actual fun readFile(path: String): String {
        val file = fopen(path, "r") ?: throw IOException("Couldn't open file $path")

        val sb = StringBuilder()
        try {
            memScoped {
                val bufferLength = 64 * 1024
                val buffer = allocArray<ByteVar>(bufferLength)

                while (true) {
                    val nextLine = fgets(buffer, bufferLength, file)?.toKString() ?: break
                    sb.append(nextLine)
                }
            }
        } finally {
            fclose(file)
        }
        return sb.toString()
    }

    actual fun fileExists(path: String): Boolean {
        val file = fopen(path, "r")

        if (file != null) {
            fclose(file)
        }
        return file != null
    }

    actual fun listFiles(path: String): List<String> {
        val dir = opendir(path) ?: throw IOException("Couldn't open directory $path")
        val entries = mutableListOf<String>()
        try {
            do {
                var ep = readdir(dir)
                while (ep != null) {
                    entries.add(ep.pointed.d_name.toKString())
                    ep = readdir(dir)
                }
            } while (ep != null)
        } finally {
            closedir(dir)
        }
        return entries
    }

    actual fun isDirectory(path: String): Boolean {
        val dir = opendir(path)

        if (dir != null) {
            closedir(dir)
            return true
        }
        return false
    }


}
