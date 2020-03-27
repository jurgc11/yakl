package uk.org.jurg.yakl.engine.utils

expect object Files {

    fun readFile(path: String): String

    fun fileExists(path: String): Boolean

    fun listFiles(path: String): List<String>

    fun isDirectory(path: String): Boolean
}
