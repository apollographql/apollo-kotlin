package com.example.enum_type.type

import javax.annotation.Generated
import kotlin.String
import kotlin.jvm.JvmStatic

/**
 * The episodes in the Star Wars trilogy (with special symbol $S)
 */
@Generated("Apollo GraphQL")
enum class Episode(val rawValue: String) {
    /**
     * Star Wars Episode IV: A New Hope, released in 1977. (with special symbol $S)
     */
    NEWHOPE("NEWHOPE"),

    /**
     * Star Wars Episode V: The Empire Strikes Back, released in 1980.
     */
    EMPIRE("EMPIRE"),

    /**
     * Star Wars Episode VI: Return of the Jedi, released in 1983. (JEDI in lowercase)
     */
    JEDI("jedi"),

    /**
     * Auto generated constant for unknown enum values
     */
    UNKNOWN__("UNKNOWN__");

    companion object {
        @JvmStatic
        fun safeValueOf(rawValue: String): Episode = values().find { it.rawValue == rawValue } ?:
                UNKNOWN__}
}
