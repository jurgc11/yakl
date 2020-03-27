package uk.org.jurg.yakl.engine.utils

import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.setenv
import platform.posix.unsetenv

actual object EnvironmentVariables {

    actual fun get(name: String): String? {
        return getenv(name)?.toKString()
    }

    actual fun set(name: String, value: String) {
        setenv(name, value, 1)
    }

    actual fun clear(name: String) {
        unsetenv(name)
    }
}
