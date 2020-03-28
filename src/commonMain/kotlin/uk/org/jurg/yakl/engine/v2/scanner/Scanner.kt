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
package uk.org.jurg.yakl.engine.v2.scanner

import uk.org.jurg.yakl.engine.v2.tokens.Token

/**
 * This interface represents an input stream of [Token]s.
 *
 * The scanner and the scanner form together the 'Parse' step in the loading process.
 *
 * @see org.snakeyaml.engine.v2.tokens.Token
 */
interface Scanner : Iterator<Token?> {
    /**
     * Check if the next token is one of the given types.
     *
     * @param choices token IDs.
     * @return `true` if the next token can be assigned to a variable
     * of at least one of the given types. Returns `false` if
     * no more tokens are available.
     * @throws ScannerException Thrown in case of malformed input.
     */
    fun checkToken(vararg choices: Token.ID): Boolean

    /**
     * Return the next token, but do not delete it from the stream.
     * The method must be called only after [.checkToken].
     *
     * @return The token that will be returned on the next call to [.next]
     * @throws ScannerException          Thrown in case of malformed input.
     * @throws IndexOutOfBoundsException if no more token left
     */
    fun peekToken(): Token

    /**
     * Returns the next token.
     *
     * The token will be removed from the stream.
     * (Every invocation of this method must happen after calling [.checkToken].
     *
     * @return the coming token
     * @throws ScannerException          Thrown in case of malformed input.
     * @throws IndexOutOfBoundsException if no more token left
     */
    override fun next(): Token
}
