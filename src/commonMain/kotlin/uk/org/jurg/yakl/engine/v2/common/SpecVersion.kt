package uk.org.jurg.yakl.engine.v2.common

data class SpecVersion(val major: Int, val minor: Int) {

    val representation: String
        get() = "$major.$minor"
}
