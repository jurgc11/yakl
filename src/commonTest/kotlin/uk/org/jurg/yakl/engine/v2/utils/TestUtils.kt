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


package uk.org.jurg.yakl.engine.v2.utils

import uk.org.jurg.yakl.engine.utils.Files
import uk.org.jurg.yakl.engine.v2.events.Event
import kotlin.test.assertEquals

object TestUtils {
    fun getResource(path: String): String {
        return Files.readFile("src/commonTest/resources$path")
    }

    fun compareEvents(
        list1: List<Event>,
        list2: List<Event>
    ) {
        assertEquals(list1.size, list2.size)
        for (n in list1.indices) {
            val ev1 = list1[n].toString()
            val ev2 = list2[n].toString()
            assertEquals(ev1, ev2)
        }
    }
}
