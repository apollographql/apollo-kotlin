package com.apollographql.apollo3.compiler

private val JAVA_RESERVED_WORDS = arrayOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default",
    "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected",
    "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "true", "void", "volatile", "while"
)

private val KOTLIN_RESERVED_WORDS = arrayOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null", "object", "package",
    "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while", "yield", "it", "field"
)

private val RESERVED_ENUM_VALUE_NAMES = arrayOf("name", "ordinal")

fun String.escapeJavaReservedWord() = if (this in JAVA_RESERVED_WORDS) "${this}_" else this

fun String.escapeKotlinReservedWord() = if (this in (JAVA_RESERVED_WORDS + KOTLIN_RESERVED_WORDS)) "${this}_" else this

fun String.escapeKotlinReservedEnumValueNames() = if (this in (JAVA_RESERVED_WORDS + KOTLIN_RESERVED_WORDS + RESERVED_ENUM_VALUE_NAMES)) "${this}_" else this
