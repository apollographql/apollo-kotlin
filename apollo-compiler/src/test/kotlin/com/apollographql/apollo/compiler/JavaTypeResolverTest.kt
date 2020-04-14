package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.*

private fun String.relativePathToGraphql(dropLast: Int = 0): String {
  val parts = split(File.separator)
      .filter { it.isNotBlank() }
      .dropLast(dropLast)

  for (i in 2 until parts.size) {
    if (parts[i - 2] == "src" && parts[i] == "graphql") {
      return parts.subList(i + 1, parts.size).joinToString(File.separator)
    }
  }

  throw Exception("$this should contain src/{foo}/graphql")
}

private fun String.formatPackageName(dropLast: Int = 0): String? {
  return relativePathToGraphql(dropLast = dropLast).split(File.separator).joinToString(".")
}



class JavaTypeResolverTest {
  private val packageNameProvider = object: PackageNameProvider {
    override val fragmentsPackageName = ""
    override val typesPackageName = ""

    override fun operationPackageName(filePath: String): String {
      return filePath.relativePathToGraphql()
    }

  }

  private val defaultContext = CodeGenerationContext(
      reservedTypeNames = emptyList(),
      typeDeclarations = emptyList(),
      packageNameProvider = packageNameProvider,
      customTypeMap = emptyMap(),
      operationIdGenerator = OperationIdGenerator.Sha256(),
      nullableValueType = NullableValueType.APOLLO_OPTIONAL,
      ir = CodeGenerationIR(emptyList(), emptyList(), emptyList()),
      useSemanticNaming = false,
      generateModelBuilder = false,
      useJavaBeansSemanticNaming = false,
      suppressRawTypesWarning = false,
      generateVisitorForPolymorphicDatatypes = false
  )
  private val defaultResolver = JavaTypeResolver(defaultContext, packageName)

  @Test
  fun resolveScalarType() {
    Assert.assertEquals(ClassNames.STRING.annotated(Annotations.NONNULL), defaultResolver.resolve("String!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(ClassNames.STRING), defaultResolver.resolve("String", true))

    Assert.assertEquals(TypeName.INT, defaultResolver.resolve("Int!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(TypeName.INT.box()), defaultResolver.resolve("Int", true))

    Assert.assertEquals(TypeName.BOOLEAN, defaultResolver.resolve("Boolean!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(TypeName.BOOLEAN.box()),
        defaultResolver.resolve("Boolean", true))

    Assert.assertEquals(TypeName.DOUBLE, defaultResolver.resolve("Float!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(TypeName.DOUBLE.box()), defaultResolver.resolve("Float", true))
  }

  @Test
  fun resolveListType() {
    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.STRING).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[String!]!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(ClassNames.parameterizedListOf(ClassNames.STRING)),
        defaultResolver.resolve("[String!]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.INT.box()).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[Int]!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(ClassNames.parameterizedListOf(TypeName.INT.box())),
        defaultResolver.resolve("[Int]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.BOOLEAN.box()).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[Boolean]!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(ClassNames.parameterizedListOf(TypeName.BOOLEAN.box())),
        defaultResolver.resolve("[Boolean]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(TypeName.DOUBLE.box()).annotated(Annotations.NONNULL),
        defaultResolver.resolve("[Float]!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(ClassNames.parameterizedListOf(TypeName.DOUBLE.box())),
        defaultResolver.resolve("[Float]", true))

    Assert.assertEquals(ClassNames.parameterizedListOf(ClassNames.parameterizedListOf(ClassNames.STRING)).annotated(
        Annotations.NONNULL),
        defaultResolver.resolve("[[String!]!]!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(
        ClassNames.parameterizedListOf(ClassNames.parameterizedListOf(ClassNames.STRING))),
        defaultResolver.resolve("[[String]]", true))
  }

  @Test
  fun resolveCustomType() {
    Assert.assertEquals(ClassName.get("", "CustomClass").annotated(Annotations.NONNULL),
        JavaTypeResolver(defaultContext, "").resolve("CustomClass!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(ClassName.get("", "CustomClass")),
        JavaTypeResolver(defaultContext, "").resolve("CustomClass", true))

    Assert.assertEquals(ClassName.get(packageName, "CustomClass").annotated(Annotations.NONNULL),
        defaultResolver.resolve("CustomClass!"))
    Assert.assertEquals(ClassNames.parameterizedOptional(ClassName.get(packageName, "CustomClass")),
        defaultResolver.resolve("CustomClass", true))
  }

  @Test
  fun resolveCustomScalarType() {
    val context = defaultContext.copy(customTypeMap = mapOf("Date" to "java.util.Date", "UnsupportedType" to "Object",
        "ID" to "java.lang.Integer"))
    Assert.assertEquals(ClassName.get(Date::class.java).annotated(Annotations.NONNULL),
        JavaTypeResolver(context, packageName).resolve("Date", false))
    Assert.assertEquals(ClassNames.parameterizedOptional(Date::class.java),
        JavaTypeResolver(context, packageName).resolve("Date", true))
    Assert.assertEquals(ClassNames.parameterizedOptional(ClassName.get("", "Object")),
        JavaTypeResolver(context, packageName).resolve("UnsupportedType", true))
    Assert.assertEquals(ClassName.get(Integer::class.java).annotated(Annotations.NONNULL),
        JavaTypeResolver(context, packageName).resolve("ID", false))
    Assert.assertEquals(ClassNames.parameterizedOptional(Integer::class.java),
        JavaTypeResolver(context, packageName).resolve("ID", true))
  }

  companion object {
    private const val packageName = "com.example"
  }
}
