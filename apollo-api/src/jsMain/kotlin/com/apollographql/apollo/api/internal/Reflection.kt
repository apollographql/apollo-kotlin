package com.apollographql.apollo.api.internal

import kotlin.reflect.KClass

actual val KClass<*>.qualifiedName2: String?
  get() = when (simpleName) {
    "String" -> "kotlin.String"
    "BoxedChar" -> "kotlin.Char"
    "Boolean" -> "kotlin.Boolean"
    "Byte" -> "kotlin.Byte"
    "Short" -> "kotlin.Short"
    "Int" -> "kotlin.Int"
    "Long" -> "kotlin.Long"
    "Float" -> "kotlin.Float"
    "Double" -> "kotlin.Double"
    "List" -> "kotlin.collections.List"
    "Map" -> "kotlin.collections.Map"
    "BigDecimal" -> "com.apollographql.apollo.api.BigDecimal"
    "FileUpload" -> "com.apollographql.apollo.api.FileUpload"
    else -> simpleName
  }