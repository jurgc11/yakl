package uk.org.jurg.yakl.engine.v2.api

import uk.org.jurg.yakl.engine.v2.common.SpecVersion
import uk.org.jurg.yakl.engine.v2.env.EnvConfig
import uk.org.jurg.yakl.engine.v2.exceptions.YamlVersionException
import uk.org.jurg.yakl.engine.v2.nodes.Tag
import uk.org.jurg.yakl.engine.v2.resolver.JsonScalarResolver
import uk.org.jurg.yakl.engine.v2.resolver.ScalarResolver

/**
 * Fine tuning parsing/loading
 * Description for all the fields can be found in the builder
 */
data class LoadSettings(
    val label: String = "reader",
    val tagConstructors: Map<Tag, ConstructNode> = mapOf(),
    val scalarResolver: ScalarResolver = JsonScalarResolver(),
    val defaultList: (Int) -> MutableList<Any?> = { ArrayList(it) },
    val defaultSet: (Int) -> MutableSet<Any?> = { LinkedHashSet(it) },
    val defaultMap: (Int) -> MutableMap<Any?, Any?> = { LinkedHashMap(it) },
    val versionFunction: (SpecVersion) -> SpecVersion = {
        if (it.major != 1) throw YamlVersionException(it) else it
    },
    val bufferSize: Int = 1024,
    val allowDuplicateKeys: Boolean = false,
    val allowRecursiveKeys: Boolean = false,
    val maxAliasesForCollections: Int = 50,
    val useMarks: Boolean = true,
    val customProperties: Map<SettingKey, Any> = mapOf(),
    val envConfig: EnvConfig? = null
)
