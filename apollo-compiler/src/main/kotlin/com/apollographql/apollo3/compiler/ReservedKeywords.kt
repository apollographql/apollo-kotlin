package com.apollographql.apollo3.compiler

// Reference:
// https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
private val JAVA_RESERVED_WORDS = arrayOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default",
    "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected",
    "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "true", "void", "volatile", "while"
)

// Note: reserved keywords escaping is handled by KotlinPoet, but "yield" is missing from its list at the moment
// so we handle it ourselves (see https://github.com/apollographql/apollo-android/issues/1957)
private val KOTLIN_RESERVED_WORDS = arrayOf("yield")

// Reference:
// https://kotlinlang.org/docs/enum-classes.html#working-with-enum-constants:~:text=properties%20for%20obtaining%20its%20name%20and%20position
private val KOTLIN_RESERVED_ENUM_VALUE_NAMES = arrayOf("name", "ordinal")

fun String.escapeJavaReservedWord() = if (this in JAVA_RESERVED_WORDS) "${this}_" else this

fun String.escapeKotlinReservedWord() = if (this in KOTLIN_RESERVED_WORDS) "`${this}`" else this

fun String.escapeKotlinReservedEnumValueNames() = if (this in (KOTLIN_RESERVED_WORDS + KOTLIN_RESERVED_ENUM_VALUE_NAMES)) "${this}_" else this
