package uk.org.jurg.yakl.engine.v2.api

/**
 * Type for the custom keys in settings. The keys (used in custom settings) must implement this interface.
 * It makes possible to be 'compiler safe' and define Enum for keys.
 * This way is easier to follow the changes in the key names and avoid a typo.
 */
interface SettingKey
