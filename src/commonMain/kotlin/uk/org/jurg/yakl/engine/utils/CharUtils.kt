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

const val MIN_HIGH_SURROGATE = '\uD800'
const val MIN_LOW_SURROGATE = '\uDC00'
const val MIN_SUPPLEMENTARY_CODE_POINT = 0x010000
const val MAX_CODE_POINT = 0X10FFFF

fun Int.codePointToChars(): CharArray {
    return when {
        this.isBmpCodePoint() -> charArrayOf(this.toChar())
        this.isValidCodePoint() -> charArrayOf(this.lowSurrogate(), this.highSurrogate())
        else -> throw IllegalArgumentException("Not a valid Unicode code point: $this")
    }
}

fun Int.codePointToString(): String {
    return String(codePointToChars())
}

fun Int.charCount(): Int {
    return when {
        this.isBmpCodePoint() -> 1
        this.isValidCodePoint() -> 2
        else -> throw IllegalArgumentException("Not a valid Unicode code point: $this")
    }
}

fun Int.isBmpCodePoint(): Boolean {
    return this ushr 16 == 0
}

fun Int.isSupplementaryCodePoint(): Boolean {
    return (this >= MIN_SUPPLEMENTARY_CODE_POINT
            && this < MAX_CODE_POINT + 1)
}

fun Int.lowSurrogate(): Char {
    return ((this and 0x3ff) + MIN_LOW_SURROGATE.toInt()).toChar()
}

fun Int.highSurrogate(): Char {
    return ((this ushr 10)
            + (MIN_HIGH_SURROGATE.toInt() - (MIN_SUPPLEMENTARY_CODE_POINT ushr 10))).toChar()
}

fun Int.isValidCodePoint(): Boolean {
    // Optimized form of:
    // codePoint >= MIN_CODE_POINT && codePoint <= MAX_CODE_POINT
    val plane = this ushr 16
    return plane < MAX_CODE_POINT + 1 ushr 16
}

fun String.toIntArray(): IntArray {
    val ints = ArrayList<Int>(this.length)
    var i = 0
    while (i < this.length) {
        val char = this[i]
        if (char.isHighSurrogate()) {
            val nextChar = this[i + 1]
            i++
            ints.add(toCodePoint(char, nextChar))
        } else {
            ints.add(char.toInt())
        }
        i++
    }
    return ints.toIntArray()
}

fun IntArray.toString(offset: Int = 0, count: Int = this.size - 1): String {
    val sb = StringBuilder(this.size)
    for (i in offset until offset + count) {
        val cp = this[i]
        if (cp.isBmpCodePoint()) {
            sb.append(cp.toChar())
        } else {
            sb.append(cp.highSurrogate()).append(cp.lowSurrogate())
        }
    }
    return sb.toString()
}

fun String.codePointAt(offset: Int): Int {
    val c1 = this[offset]
    if (c1.isHighSurrogate()) {
        val c2 = this[offset + 1]
        return toCodePoint(c1, c2)
    }
    return c1.toInt()
}

fun CharArray.codePointAt(offset: Int): Int {
    val c1 = this[offset]
    if (c1.isHighSurrogate()) {
        val c2 = this[offset + 1]
        return toCodePoint(c1, c2)
    }
    return c1.toInt()
}

fun toCodePoint(high: Char, low: Char): Int {

    // Optimized form of:
    // return ((high - MIN_HIGH_SURROGATE) << 10)
    //         + (low - MIN_LOW_SURROGATE)
    //         + MIN_SUPPLEMENTARY_CODE_POINT;
    return (high.toInt() shl 10) + low.toInt() +
            (MIN_SUPPLEMENTARY_CODE_POINT - (MIN_HIGH_SURROGATE.toInt() shl 10) - MIN_LOW_SURROGATE.toInt())
}

fun Int.isPrintable(): Boolean {
    return (this in 0x20..0x7E ||
            this == 0x9 ||
            this == 0xA ||
            this == 0xD ||
            this == 0x85 ||
            this in 0xA0..0xD7FF ||
            this in 0xE000..0xFFFD ||
            this in 0x10000..0x10FFFF)
}

fun Char.isPrintable(): Boolean {
    return this.toInt().isPrintable()
}

fun String.isPrintable(): Boolean {
    return this.toIntArray().none { !it.isPrintable() }
}

fun Int.isDigit(): Boolean {
    return this in 0x0030..0x0039 ||
        this in 0x0660..0x0669 ||
        this in 0x06F0..0x06F9 ||
        this in 0x0966..0x096F ||
        this in 0x09E6..0x09EF ||
        this in 0x0A66..0x0A6F ||
        this in 0x0AE6..0x0AEF ||
        this in 0x0B66..0x0B6F ||
        this in 0x0BE7..0x0BEF ||
        this in 0x0C66..0x0C6F ||
        this in 0x0CE6..0x0CEF ||
        this in 0x0D66..0x0D6F ||
        this in 0x0E50..0x0E59 ||
        this in 0x0ED0..0x0ED9 ||
        this in 0xFF10..0xFF19
}
