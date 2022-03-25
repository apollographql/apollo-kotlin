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


// Reference:
// https://kotlinlang.org/docs/enum-classes.html#working-with-enum-constants:~:text=properties%20for%20obtaining%20its%20name%20and%20position
private val KOTLIN_RESERVED_ENUM_VALUE_NAMES = arrayOf("name", "ordinal")

fun String.escapeJavaReservedWord() = if (this in JAVA_RESERVED_WORDS) "${this}_" else this

// Does nothing. KotlinPoet will add the backticks
fun String.escapeKotlinReservedWord() = this

fun String.escapeKotlinReservedEnumValueNames() = if (this in KOTLIN_RESERVED_ENUM_VALUE_NAMES) "${this}_" else this
