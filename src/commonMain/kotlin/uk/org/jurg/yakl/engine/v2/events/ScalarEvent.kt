package uk.org.jurg.yakl.engine.v2.events

import uk.org.jurg.yakl.engine.utils.toIntArray
import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.common.CharConstants
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.exceptions.Mark

class ScalarEvent(
    anchor: Anchor?,
    val tag: String?,
    val implicit: ImplicitTuple,
    val value: String,
    val scalarStyle: ScalarStyle,
    startMark: Mark? = null,
    endMark: Mark? = null
) : NodeEvent(anchor, startMark, endMark) {

    val escapesToPrint = CharConstants.ESCAPES
        .filter { it.key != '"' }
        .toMap()

    fun isPlain(): Boolean {
        return scalarStyle === ScalarStyle.PLAIN
    }

    override val eventId: ID
        get() = ID.Scalar

    override fun toString(): String {
        val builder = StringBuilder("=VAL")
        if (anchor != null) {
            builder.append(" &").append(anchor)
        }
        if (implicit.bothFalse() && tag != null) {
            builder.append(" <").append(tag).append(">")
        }
        builder.append(" ")
        builder.append(scalarStyle.toString())
        builder.append(escapedValue())
        return builder.toString()
    }

    /*
     * Escape char (prepending '\')
     * ch - the character to escape. Surrogates are not supported (because of int -> char conversion)
     */
    private fun escape(ch: Char): String? {
        val print = escapesToPrint[ch]
        return if (print != null) {
            "\\"+print.toChar()
        } else {
            ch.toString()
        }
    }

    //escape and drop surrogates
    fun escapedValue(): String {
        return value.toIntArray()
            .filter { it < Char.MAX_VALUE.toInt() }
            .map { escape(it.toChar()) }
            .joinToString(separator = "")
    }
}
