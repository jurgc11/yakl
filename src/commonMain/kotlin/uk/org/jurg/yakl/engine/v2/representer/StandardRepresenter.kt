/*
 * Copyright (c) 2018, http://www.snakeyaml.org
 * Copyright (c) 2020, Chris Clifton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.jurg.yakl.engine.v2.representer

import com.benasher44.uuid.Uuid
import uk.org.jurg.yakl.engine.utils.base64EncodeToString
import uk.org.jurg.yakl.engine.utils.isPrintable
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import uk.org.jurg.yakl.engine.v2.api.RepresentToNode
import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.common.NonPrintableStyle
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.Node
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import kotlin.reflect.KClass

class StandardRepresenter(private val settings: DumpSettings) : BaseRepresenter() {

    protected var classTags: MutableMap<KClass<out Any>, Tag> = mutableMapOf()

    init {
        this.defaultFlowStyle = settings.defaultFlowStyle
        this.defaultScalarStyle = settings.defaultScalarStyle

        nullRepresenter = RepresentNull()

        val primitiveArray: RepresentToNode = RepresentPrimitiveArray()
        representers[ShortArray::class] = primitiveArray
        representers[IntArray::class] = primitiveArray
        representers[LongArray::class] = primitiveArray
        representers[FloatArray::class] = primitiveArray
        representers[DoubleArray::class] = primitiveArray
        representers[CharArray::class] = primitiveArray
        representers[BooleanArray::class] = primitiveArray
        representers[String::class] = RepresentString()
        representers[Boolean::class] = RepresentBoolean()
        representers[Char::class] = RepresentString()
        representers[Uuid::class] = RepresentUuid()
        representers[ByteArray::class] = RepresentByteArray()
//        representers[Optional::class] = RepresentOptional()

        parentClassRepresenters[Number::class] = RepresentNumber()
        parentClassRepresenters[List::class] = RepresentList()
        parentClassRepresenters[Map::class] = RepresentMap()
        parentClassRepresenters[Set::class] = RepresentSet()
        parentClassRepresenters[Iterator::class] = RepresentIterator()
        parentClassRepresenters[Array<Any>::class] = RepresentArray()
        parentClassRepresenters[Enum::class] = RepresentEnum()
    }

    protected fun getTag(
        clazz: KClass<*>,
        defaultTag: Tag
    ): Tag {
        return classTags[clazz] ?: defaultTag
    }

    inner class RepresentString : RepresentToNode {
        private val multilinePattern = "[\n\u0085\u2028\u2029]".toRegex()

        @ExperimentalStdlibApi
        override fun representData(data: Any?): Node {
            var tag = Tag.STR
            var style = ScalarStyle.PLAIN
            var value = data.toString()
            if (settings.nonPrintableStyle == NonPrintableStyle.BINARY && !value.isPrintable()) {
                tag = Tag.BINARY
                val bytes: ByteArray = value.encodeToByteArray(throwOnInvalidSequence = true)

                value = bytes.base64EncodeToString()
                style = ScalarStyle.LITERAL
            }
            // if no other scalar style is explicitly set, use literal style for
            // multiline scalars
            if (settings.defaultScalarStyle === ScalarStyle.PLAIN && multilinePattern.containsMatchIn(value)) {
                style = ScalarStyle.LITERAL
            }
            return representScalar(tag, value, style)
        }
    }

    inner class RepresentBoolean : RepresentToNode {
        override fun representData(data: Any?): Node {
            val value = if (true == data) {
                "true"
            } else {
                "false"
            }
            return representScalar(Tag.BOOL, value)
        }
    }

    inner class RepresentNull : RepresentToNode {
        override fun representData(data: Any?): Node {
            return representScalar(Tag.NULL, "null")
        }
    }

    inner class RepresentNumber : RepresentToNode{

        override fun representData(data: Any?): Node {
            val tag: Tag
            val value: String
            when (data) {
                is Byte, is Short, is Int, is Long -> {
                    tag = Tag.INT
                    value = data.toString()
                }
                else -> {
                    val number = data as Number
                    tag = Tag.FLOAT
                    value = when (number) {
                        Double.NaN -> ".NaN"
                        Double.POSITIVE_INFINITY -> ".inf"
                        Double.NEGATIVE_INFINITY -> "-.inf"
                        else -> number.toString()
                    }
                }
            }
            return representScalar(getTag(data::class, tag), value)
        }
    }

    inner class RepresentList : RepresentToNode {
        override fun representData(data: Any?): Node {
            return representSequence(
                getTag(data!!::class, Tag.SEQ),
                data as List<Any?>,
                FlowStyle.AUTO
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    inner class RepresentMap : RepresentToNode {
        override fun representData(data: Any?): Node {
            return representMapping(
                getTag(data!!::class, Tag.MAP), data as Map<Any?, Any?>,
                FlowStyle.AUTO
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    inner class RepresentSet : RepresentToNode {
        override fun representData(data: Any?): Node {
            val value: MutableMap<Any, Any?> = LinkedHashMap()
            val set = data as Set<Any>
            for (key in set) {
                value[key] = null
            }
            return representMapping(
                getTag(data::class, Tag.SET),
                value,
                FlowStyle.AUTO
            )
        }
    }

    inner class RepresentEnum : RepresentToNode {
        override fun representData(data: Any?): Node {
            val tag = Tag(data!!::class)
            return representScalar(getTag(data::class, tag), (data as Enum<*>).name)
        }
    }

    inner class RepresentByteArray : RepresentToNode {
        override fun representData(data: Any?): Node {
            data as ByteArray
            return representScalar(
                Tag.BINARY,
                data.base64EncodeToString(),
                ScalarStyle.LITERAL
            )
        }
    }

    inner class RepresentUuid : RepresentToNode {
        override fun representData(data: Any?): Node {
            return representScalar(
                getTag(data!!::class, Tag(Uuid::class)),
                data.toString()
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    inner class RepresentIterator : RepresentToNode {
        override fun representData(data: Any?): Node {
            val iter = data as Iterator<Any>
            return representSequence(
                getTag(data::class, Tag.SEQ),
                IteratorWrapper(iter),
                FlowStyle.AUTO
            )
        }
    }

    private class IteratorWrapper(private val iter: Iterator<Any>) : Iterable<Any?> {
        override fun iterator(): Iterator<Any> {
            return iter
        }

    }

    @Suppress("UNCHECKED_CAST")
    inner class RepresentArray : RepresentToNode {
        override fun representData(data: Any?): Node {
            val array = data as Array<Any>
            return representSequence(Tag.SEQ, array.asList(), FlowStyle.AUTO)
        }
    }

    /**
     * Represents primitive arrays, such as short[] and float[], by converting
     * them into equivalent [List] using the appropriate
     * autoboxing type.
     */
    inner class RepresentPrimitiveArray : RepresentToNode {
        override fun representData(data: Any?): Node {
            return when (data) {
                is ByteArray -> representSequence(Tag.SEQ, data.toList(), FlowStyle.AUTO)
                is ShortArray -> representSequence(Tag.SEQ, data.toList(), FlowStyle.AUTO)
                is IntArray -> representSequence(Tag.SEQ, data.toList(), FlowStyle.AUTO)
                is LongArray -> representSequence(Tag.SEQ, data.toList(), FlowStyle.AUTO)
                is FloatArray -> representSequence(Tag.SEQ, data.toList(), FlowStyle.AUTO)
                is DoubleArray -> representSequence(Tag.SEQ, data.toList(), FlowStyle.AUTO)
                is CharArray -> representSequence(Tag.SEQ, data.toList(), FlowStyle.AUTO)
                is BooleanArray -> representSequence(Tag.SEQ, data.toList(), FlowStyle.AUTO)
                else -> throw YamlEngineException("Unexpected primitive '${data?.let { it::class.simpleName}}'")
            }
        }
    }
}
