package uk.org.jurg.yakl.engine.utils

expect object EnvironmentVariables {
    fun get(name: String): String?
    fun set(name: String, value: String)
    fun clear(name: String)
}
