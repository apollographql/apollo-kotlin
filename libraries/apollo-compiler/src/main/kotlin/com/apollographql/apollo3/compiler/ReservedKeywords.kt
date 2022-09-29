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

fun String.escapeJavaReservedWord() = if (this in JAVA_RESERVED_WORDS) "${this}_" else this

// Does nothing. KotlinPoet will add the backticks
fun String.escapeKotlinReservedWord() = this

internal fun String.escapeTypeReservedWord(): String? {
  return when {
    // type is forbidden because we use it as a companion property to hold the CompiledType
    // See https://github.com/apollographql/apollo-kotlin/issues/4293
    "(?:type)_*".toRegex().matches(this) -> "${this}_"
    else -> null
  }
}


fun String.escapeKotlinReservedWordInEnum(): String {
  return when {
    // name and ordinal are forbidden because already used in Kotlin
    // See https://kotlinlang.org/docs/enum-classes.html#working-with-enum-constants:~:text=properties%20for%20obtaining%20its%20name%20and%20position
    "(?:name|ordinal)_*".toRegex().matches(this) -> "${this}_"
    else -> escapeTypeReservedWord() ?: escapeKotlinReservedWord()
  }
}

fun String.escapeKotlinReservedWordInSealedClass(): String {
  return escapeTypeReservedWord() ?: escapeKotlinReservedWord()
}
