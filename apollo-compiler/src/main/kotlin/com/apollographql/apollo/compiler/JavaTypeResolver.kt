package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ClassNames.parameterizedGuavaOptional
import com.apollographql.apollo.compiler.ClassNames.parameterizedJavaOptional
import com.apollographql.apollo.compiler.ClassNames.parameterizedOptional
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

class JavaTypeResolver(
    private val context: CodeGenerationContext,
    private val packageName: String,
    private val deprecated: Boolean = false
) {
  fun resolve(typeName: String, isOptional: Boolean = !typeName.endsWith("!")): TypeName {
    val normalizedTypeName = typeName.removeSuffix("!")
    val isList = normalizedTypeName.startsWith('[') && normalizedTypeName.endsWith(']')
    val customScalarType = context.customTypeMap[normalizedTypeName]
    val javaType = when {
      isList -> ClassNames.parameterizedListOf(resolve(normalizedTypeName.removeSurrounding("[", "]"), false))
      normalizedTypeName == "String" -> ClassNames.STRING
      normalizedTypeName == "ID" -> ClassNames.STRING
      normalizedTypeName == "Int" -> if (isOptional) TypeName.INT.box() else TypeName.INT
      normalizedTypeName == "Boolean" -> if (isOptional) TypeName.BOOLEAN.box() else TypeName.BOOLEAN
      normalizedTypeName == "Float" -> if (isOptional) TypeName.DOUBLE.box() else TypeName.DOUBLE
      customScalarType != null -> customScalarType.toJavaType()
      else -> ClassName.get(packageName, normalizedTypeName)
    }

    return if (javaType.isPrimitive) {
      javaType.let { if (deprecated) it.annotated(Annotations.DEPRECATED) else it }
    } else if (isOptional) {
      when (context.nullableValueType) {
        NullableValueType.APOLLO_OPTIONAL -> parameterizedOptional(javaType)
        NullableValueType.GUAVA_OPTIONAL -> parameterizedGuavaOptional(javaType)
        NullableValueType.JAVA_OPTIONAL -> parameterizedJavaOptional(javaType)
        else -> javaType.annotated(Annotations.NULLABLE)
      }.let {
        if (deprecated) it.annotated(Annotations.DEPRECATED) else it
      }
    } else {
      javaType.annotated(Annotations.NONNULL).let {
        if (deprecated) it.annotated(Annotations.DEPRECATED) else it
      }
    }
  }
}