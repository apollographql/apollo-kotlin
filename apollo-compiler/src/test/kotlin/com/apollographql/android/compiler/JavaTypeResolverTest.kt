package com.apollographql.android.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Assert
import org.junit.Test
import java.util.*

class JavaTypeResolverTest {
  private val defaultResolver = JavaTypeResolver(emptyMap(), packageName)

  @Test
  fun resolveScalarType() {
    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NONNULL), defaultResolver.resolve("String", false))
    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NULLABLE), defaultResolver.resolve("String", true))

    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NONNULL), defaultResolver.resolve("ID", false))
    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NULLABLE), defaultResolver.resolve("ID", true))

    Assert.assertEquals(TypeName.INT, defaultResolver.resolve("Int", false))
    Assert.assertEquals(TypeName.INT.box().annotated(Annotations.NULLABLE), defaultResolver.resolve("Int", true))

    Assert.assertEquals(TypeName.BOOLEAN, defaultResolver.resolve("Boolean", false))
    Assert.assertEquals(TypeName.BOOLEAN.box().annotated(Annotations.NULLABLE),
        defaultResolver.resolve("Boolean", true))

    Assert.assertEquals(TypeName.DOUBLE, defaultResolver.resolve("Float", false))
    Assert.assertEquals(TypeName.DOUBLE.box().annotated(Annotations.NULLABLE), defaultResolver.resolve("Float", true))
  }

  @Test
  fun resolveListType() {
    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[String!]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NULLABLE),
        defaultResolver.resolve("[String!]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[ID]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NULLABLE),
        defaultResolver.resolve("[ID]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.INT.box()).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[Int]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.INT.box()).annotated(Annotations.NULLABLE),
        defaultResolver.resolve("[Int]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.BOOLEAN.box()).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[Boolean]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.BOOLEAN.box()).annotated(Annotations.NULLABLE),
        defaultResolver.resolve("[Boolean]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.DOUBLE.box()).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[Float]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.DOUBLE.box()).annotated(Annotations.NULLABLE),
        defaultResolver.resolve("[Float]", true))
  }

  @Test
  fun resolveCustomType() {
    Assert.assertEquals(ClassName.get("", "CustomClass").annotated(Annotations.NONNULL),
        JavaTypeResolver(emptyMap(), "").resolve("CustomClass", false))
    Assert.assertEquals(ClassName.get("", "CustomClass").annotated(Annotations.NULLABLE),
        JavaTypeResolver(emptyMap(), "").resolve("CustomClass", true))

    Assert.assertEquals(ClassName.get(packageName, "CustomClass").annotated(Annotations.NONNULL),
        defaultResolver.resolve("CustomClass", false))
    Assert.assertEquals(ClassName.get(packageName, "CustomClass").annotated(Annotations.NULLABLE),
        defaultResolver.resolve("CustomClass", true))
  }

  @Test
  fun resolveCustomScalarType() {
    val customScalarTypeMap = mapOf("Date" to "java.util.Date", "UnsupportedType" to "Object")
    Assert.assertEquals(ClassName.get(Date::class.java).annotated(Annotations.NONNULL),
        JavaTypeResolver(customScalarTypeMap, packageName).resolve("Date", false))
    Assert.assertEquals(ClassName.get(Date::class.java).annotated(Annotations.NULLABLE),
        JavaTypeResolver(customScalarTypeMap, packageName).resolve("Date", true))
    Assert.assertEquals(ClassName.get("", "Object").annotated(Annotations.NULLABLE),
        JavaTypeResolver(customScalarTypeMap, packageName).resolve("UnsupportedType", true))
  }

  companion object {
    private const val packageName = "com.example"
  }
}