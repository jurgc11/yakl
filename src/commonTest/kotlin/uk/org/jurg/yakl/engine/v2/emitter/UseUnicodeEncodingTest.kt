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


package uk.org.jurg.yakl.engine.v2.emitter

import uk.org.jurg.yakl.engine.v2.api.Dump
import uk.org.jurg.yakl.engine.v2.api.DumpSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class UseUnicodeEncodingTest {
    @Test
    fun testEmitUnicode() {
        val settings = DumpSettings()
        val dump = Dump(settings)
        val russianUnicode = "Пушкин - это наше всё! 😊"
        val actual = dump.dumpToString(russianUnicode)
        assertEquals("$russianUnicode\n", actual)
    }

    @Test
    fun testEscapeUnicode() {
        val settings: DumpSettings = DumpSettings(
            useUnicodeEncoding = false
        )
        val dump = Dump(settings)
        val russianUnicode = "Пушкин - это наше всё! 😊"
        assertEquals(
            """"\u041f\u0443\u0448\u043a\u0438\u043d - \u044d\u0442\u043e \u043d\u0430\u0448\u0435\
  \ \u0432\u0441\u0451! \U0001f60a"
""", dump.dumpToString(russianUnicode)
        )
    }
}
