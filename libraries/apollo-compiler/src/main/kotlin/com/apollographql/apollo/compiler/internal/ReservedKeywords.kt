package com.apollographql.apollo.compiler.internal

// Reference:
// https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
private val JAVA_RESERVED_WORDS = arrayOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default",
    "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected",
    "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "true", "void", "volatile", "while"
)

private val TYPE_REGEX = "(?:type)_*".toRegex()
private val COMPANION_REGEX = "(?:Companion)_*".toRegex()
private val KOTLIN_ENUM_RESERVED_WORDS_REGEX = "(?:name|ordinal)_*".toRegex()

internal fun String.escapeJavaReservedWord() = if (this in JAVA_RESERVED_WORDS) "${this}_" else this

// Does nothing. KotlinPoet will add the backticks
internal fun String.escapeKotlinReservedWord() = this

internal fun String.escapeTypeReservedWord(): String? {
  return when {
    // type is forbidden because we use it as a companion property to hold the CompiledType
    // See https://github.com/apollographql/apollo-kotlin/issues/4293
    TYPE_REGEX.matches(this) -> "${this}_"
    else -> null
  }
}

private fun String.escapeCompanionReservedWord(): String? {
  return when {
    // Companion is forbidden because a Companion class is generated in enum and sealed classes
    // See https://github.com/apollographql/apollo-kotlin/issues/4557
    COMPANION_REGEX.matches(this) -> "${this}_"
    else -> null
  }
}

internal fun String.escapeKotlinReservedWordInEnum(): String {
  return when {
    // name and ordinal are forbidden because already used in Kotlin
    // See https://kotlinlang.org/docs/enum-classes.html#working-with-enum-constants:~:text=properties%20for%20obtaining%20its%20name%20and%20position
    KOTLIN_ENUM_RESERVED_WORDS_REGEX.matches(this) -> "${this}_"
    else -> escapeTypeReservedWord() ?: escapeCompanionReservedWord() ?: escapeKotlinReservedWord()
  }
}

internal fun String.escapeKotlinReservedWordInSealedClass(): String {
  return escapeTypeReservedWord() ?: escapeCompanionReservedWord() ?: escapeKotlinReservedWord()
}
