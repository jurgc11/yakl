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
package uk.org.jurg.yakl.engine.v2.api

import uk.org.jurg.yakl.engine.v2.common.FlowStyle
import uk.org.jurg.yakl.engine.v2.common.NonPrintableStyle
import uk.org.jurg.yakl.engine.v2.common.ScalarStyle
import uk.org.jurg.yakl.engine.v2.common.SpecVersion
import uk.org.jurg.yakl.engine.v2.exceptions.YamlEngineException
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.resolver.JsonScalarResolver
import uk.org.jurg.yakl.engine.v2.resolver.ScalarResolver
import uk.org.jurg.yakl.engine.v2.serializer.AnchorGenerator
import uk.org.jurg.yakl.engine.v2.serializer.NumberAnchorGenerator


/**
 * Fine tuning serializing/dumping
 * Description for all the fields can be found in the builder
 */
data class DumpSettings (
    val explicitStart: Boolean = false,
    val explicitEnd: Boolean = false,
    val nonPrintableStyle: NonPrintableStyle = NonPrintableStyle.ESCAPE,  //emitter
    val explicitRootTag: Tag? = null,
    val anchorGenerator: AnchorGenerator = NumberAnchorGenerator(1),
    val yamlDirective: SpecVersion? = null,
    val tagDirective: Map<String, String> = mapOf(),
    val scalarResolver: ScalarResolver = JsonScalarResolver(),
    val defaultFlowStyle: FlowStyle = FlowStyle.AUTO,
    val defaultScalarStyle: ScalarStyle = ScalarStyle.PLAIN,
    val canonical: Boolean = false,
    val multiLineFlow: Boolean = false,
    val useUnicodeEncoding: Boolean = true,
    val indent: Int = 2,
    val indicatorIndent: Int = 0,
    val width: Int = 80,
    val bestLineBreak: String = "\n",
    val splitLines: Boolean = true,
    val maxSimpleKeyLength: Int = 128,
    val customProperties: Map<SettingKey, Any> = mapOf()
) {
    init {
        if (maxSimpleKeyLength > 1024) {
            throw YamlEngineException("The simple key must not span more than 1024 stream characters. See https://yaml.org/spec/1.2/spec.html#id2798057")
        }
    }
}
