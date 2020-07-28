package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.FileUpload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReflectionTest {
  @Test
  fun qualifiedName2_mapping() {
    assertEquals("kotlin.String", String::class.qualifiedName2 )
    assertEquals("kotlin.Char", Char::class.qualifiedName2 )
    assertEquals("kotlin.Boolean", Boolean::class.qualifiedName2 )
    assertEquals("kotlin.Byte", Byte::class.qualifiedName2 )
    assertEquals("kotlin.Short", Short::class.qualifiedName2 )
    assertEquals("kotlin.Int", Int::class.qualifiedName2 )
    assertEquals("kotlin.Long", Long::class.qualifiedName2 )
    assertEquals("kotlin.Float", Float::class.qualifiedName2 )
    assertEquals("kotlin.Double", Double::class.qualifiedName2 )
    assertEquals("kotlin.collections.List", List::class.qualifiedName2 )
    assertEquals("kotlin.collections.Map", Map::class.qualifiedName2 )
    assertTrue { BigDecimal::class.qualifiedName2  in listOf("java.math.BigDecimal", "com.apollographql.apollo.api.BigDecimal") }
    assertEquals("com.apollographql.apollo.api.FileUpload", FileUpload::class.qualifiedName2 )
  }
}