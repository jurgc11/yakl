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


package uk.org.jurg.yakl.engine.v2.nodes

import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import kotlin.test.Test
import kotlin.test.assertTrue

class MappingNodeTest {

    @Test
    fun testToString() {
        val tuple1 = NodeTuple(
            ScalarNode(tag = Tag.STR, value = "a", style = ScalarStyle.PLAIN),
            ScalarNode(tag = Tag.INT, value = "1", style = ScalarStyle.PLAIN)
        )
        val list = ArrayList<NodeTuple>()
        list.add(tuple1)
        val mapping = MappingNode(tag = Tag.MAP, value = list, flowStyle = FlowStyle.FLOW)
        val tuple2 = NodeTuple(ScalarNode(tag = Tag.STR, value = "self", style = ScalarStyle.PLAIN), mapping)
        list.add(tuple2)
        val representation = mapping.toString()
        assertTrue(representation.startsWith("<uk.org.jurg.yakl.engine.v2.nodes.MappingNode " +
                "(tag=tag:yaml.org,2002:map, values={ key=<uk.org.jurg.yakl.engine.v2.nodes.ScalarNode " +
                "(tag=tag:yaml.org,2002:str, value=a)>; value=<NodeTuple keyNode=<uk.org.jurg.yakl.engine.v2.nodes.ScalarNode " +
                "(tag=tag:yaml.org,2002:str, value=a)>; valueNode=<uk.org.jurg.yakl.engine.v2.nodes.ScalarNode " +
                "(tag=tag:yaml.org,2002:int, value=1)>> }{ key=<uk.org.jurg.yakl.engine.v2.nodes.ScalarNode " +
                "(tag=tag:yaml.org,2002:str, value=self)>;"))
    }
}
