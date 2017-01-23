package com.apollographql.android.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Assert
import org.junit.Test

class JavaTypeResolverTest {

  @Test
  fun resolveScalarType() {
    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NONNULL),
        JavaTypeResolver(packageName).resolve("String", false))
    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("String", true))

    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NONNULL),
        JavaTypeResolver(packageName).resolve("ID", false))
    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("ID", true))

    Assert.assertEquals(TypeName.INT, JavaTypeResolver(packageName).resolve("Int", false))
    Assert.assertEquals(TypeName.INT.box().annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("Int", true))

    Assert.assertEquals(TypeName.BOOLEAN, JavaTypeResolver(packageName).resolve("Boolean", false))
    Assert.assertEquals(TypeName.BOOLEAN.box().annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("Boolean", true))

    Assert.assertEquals(TypeName.DOUBLE, JavaTypeResolver(packageName).resolve("Float", false))
    Assert.assertEquals(TypeName.DOUBLE.box().annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("Float", true))
  }

  @Test
  fun resolveListType() {
    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NONNULL),
        JavaTypeResolver(packageName).resolve("[String!]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("[String!]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NONNULL),
        JavaTypeResolver(packageName).resolve("[ID]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("[ID]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.INT.box()).annotated(Annotations.NONNULL),
        JavaTypeResolver(packageName).resolve("[Int]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.INT.box()).annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("[Int]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.BOOLEAN.box()).annotated(Annotations.NONNULL),
        JavaTypeResolver(packageName).resolve("[Boolean]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.BOOLEAN.box()).annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("[Boolean]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.DOUBLE.box()).annotated(Annotations.NONNULL),
        JavaTypeResolver(packageName).resolve("[Float]", false))
    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.DOUBLE.box()).annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("[Float]", true))
  }

  @Test
  fun resolveCustomType() {
    Assert.assertEquals(ClassName.get("", "CustomClass").annotated(Annotations.NONNULL),
        JavaTypeResolver("").resolve("CustomClass", false))
    Assert.assertEquals(ClassName.get("", "CustomClass").annotated(Annotations.NULLABLE),
        JavaTypeResolver("").resolve("CustomClass", true))

    Assert.assertEquals(ClassName.get(packageName, "CustomClass").annotated(Annotations.NONNULL),
        JavaTypeResolver(packageName).resolve("CustomClass", false))
    Assert.assertEquals(ClassName.get(packageName, "CustomClass").annotated(Annotations.NULLABLE),
        JavaTypeResolver(packageName).resolve("CustomClass", true))
  }

  companion object {
    private const val packageName = "com.example"
  }
}