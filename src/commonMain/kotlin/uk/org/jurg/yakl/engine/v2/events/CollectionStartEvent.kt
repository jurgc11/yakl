package uk.org.jurg.yakl.engine.v2.events

import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.exceptions.Mark

abstract class CollectionStartEvent(
    anchor: Anchor?,
    val tag: String?,
    val implicit: Boolean,
    val flowStyle: FlowStyle,
    startMark: Mark?,
    endMark: Mark?
) : NodeEvent(anchor, startMark, endMark) {

    open fun isFlow(): Boolean {
        return FlowStyle.FLOW === flowStyle
    }

    override fun toString(): String {
        val builder = StringBuilder()
        if (anchor != null) {
            builder.append(" &").append(anchor)
        }
        if (!implicit && tag != null) {
            builder.append(" <").append(tag).append(">")
        }
        return builder.toString()
    }
}
