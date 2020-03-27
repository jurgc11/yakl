/**
 * Copyright (c) 2018, http://www.snakeyaml.org
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.jurg.yakl.engine.usecases.external_test_suite

import uk.org.jurg.yakl.engine.v2.common.Anchor
import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.events.AliasEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentEndEvent
import uk.org.jurg.yakl.engine.v2.events.DocumentStartEvent
import uk.org.jurg.yakl.engine.v2.events.Event
import uk.org.jurg.yakl.engine.v2.events.ImplicitTuple
import uk.org.jurg.yakl.engine.v2.events.MappingStartEvent
import uk.org.jurg.yakl.engine.v2.events.ScalarEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceEndEvent
import uk.org.jurg.yakl.engine.v2.events.SequenceStartEvent
import uk.org.jurg.yakl.engine.v2.events.StreamEndEvent
import uk.org.jurg.yakl.engine.v2.events.StreamStartEvent
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventRepresentationTest {
    @Test
    fun `Represent StreamStartEvent`() {
        val event = StreamStartEvent()
        val representation = EventRepresentation(event)
        assertTrue(representation.isSameAs("+STR"))
        assertFalse(representation.isSameAs("-STR"))
        assertFalse(representation.isSameAs("=VAL"))
    }

    @Test
    fun `Represent StreamEndEvent`() {
        val event = StreamEndEvent()
        val representation = EventRepresentation(event)
        assertTrue(representation.isSameAs("-STR"))
        assertFalse(representation.isSameAs("+STR"))
    }

    @Test
    fun `Represent AliasEvent`() {
        val event = AliasEvent(Anchor("a"))
        val representation = EventRepresentation(event)
        assertTrue(representation.isSameAs("=ALI *a"))
        assertTrue(representation.isSameAs("=ALI *b"))
        assertTrue(representation.isSameAs("=ALI *002"))
        assertFalse(representation.isSameAs("=ALI &002"))
        assertFalse(representation.isSameAs("+STR"))
    }

    @Test
    fun `Represent DocumentStartEvent`() {
        valid(DocumentStartEvent(true, null, emptyMap()), "+DOC ---")
        valid(DocumentStartEvent(true, null, emptyMap()), "+DOC")
        valid(DocumentStartEvent(false, null, emptyMap()), "+DOC")
        valid(DocumentStartEvent(false, null, emptyMap()), "+DOC ---")
    }

    @Test
    fun `Represent DocumentEndEvent`() {
        valid(DocumentEndEvent(true), "-DOC ...")
        valid(DocumentEndEvent(true), "-DOC")
        invalid(DocumentEndEvent(true), "+DOC ---")
    }

    @Test
    fun `Represent SequenceStartEvent`() {
        valid(SequenceStartEvent(Anchor("a"), "ttt", false, FlowStyle.FLOW), "+SEQ &a <ttt>")
        invalid(SequenceStartEvent(Anchor("a"), "ttt", false, FlowStyle.FLOW), "+SEQ *a <ttt>")
        invalid(SequenceStartEvent(Anchor("a"), "ttt", false, FlowStyle.FLOW), "+SEQ &a <t>")
        invalid(SequenceStartEvent(Anchor("a"), "ttt", false, FlowStyle.FLOW), "+SEQ <ttt>")
        invalid(SequenceStartEvent(Anchor("a"), "ttt", false, FlowStyle.FLOW), "+SEQ *a")
    }

    @Test
    fun `Represent SequenceEndEvent`() {
        valid(SequenceEndEvent(), "-SEQ")
        invalid(SequenceEndEvent(), "-MAP")
    }

    @Test
    fun `Represent ScalarEvent`() {
        val tuple = ImplicitTuple(plain = false, nonPlain = false)
        valid(ScalarEvent(Anchor("a"), "ttt", tuple, "v1", ScalarStyle.FOLDED), "=VAL &a <ttt> >v1")
        invalid(ScalarEvent(Anchor("a"), "ttt", tuple, "v1", ScalarStyle.PLAIN), "=VAL <ttt> >v1")
        invalid(ScalarEvent(Anchor("a"), "ttt", tuple, "v1", ScalarStyle.PLAIN), "=VAL &a >v1")
        invalid(ScalarEvent(Anchor("a"), "ttt", tuple, "v1", ScalarStyle.PLAIN), "=VAL &a <ttt>")
        invalid(ScalarEvent(Anchor("a"), "ttt", tuple, "v1", ScalarStyle.PLAIN), "=VAL &a <ttt> |v1")
    }

    @Test
    fun `Represent MappingStartEvent`() {
        invalid(MappingStartEvent(Anchor("a"), "ttt", false, FlowStyle.FLOW), "+MAP")
        valid(MappingStartEvent(null, Tag.MAP.value, false, FlowStyle.FLOW), "+MAP")
        valid(MappingStartEvent(null, null, false, FlowStyle.FLOW), "+MAP")
    }

    private fun valid(event: Event, expectation: String) {
        val representation = EventRepresentation(event)
        assertTrue(representation.isSameAs(expectation))
    }

    private fun invalid(event: Event, expectation: String) {
        val representation = EventRepresentation(event)
        assertFalse(representation.isSameAs(expectation))
    }
}
