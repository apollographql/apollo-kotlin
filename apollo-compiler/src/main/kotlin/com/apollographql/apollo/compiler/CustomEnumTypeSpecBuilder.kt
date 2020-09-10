package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.lang.model.element.Modifier

class CustomEnumTypeSpecBuilder(
    val context: CodeGenerationContext,
    val scalarTypes: Set<String>
) {
  fun build(): TypeSpec =
      TypeSpec.enumBuilder(className(context))
          .addSuperinterface(ScalarType::class.java)
          .addModifiers(Modifier.PUBLIC)
          .addEnumConstants()
          .build()

  private fun TypeSpec.Builder.addEnumConstants(): TypeSpec.Builder {
    context.customTypeMap
        .filterKeys { scalarTypes.contains(it) }
        .toSortedMap()
        .forEach { mapping ->
          val constantName = mapping.key.removeSuffix("!").toUpperCase(Locale.ENGLISH)
          val javaTypeName = mapping.value
          addEnumConstant(constantName, scalarMappingTypeSpec(mapping.key, javaTypeName))
        }
    return this
  }

  private fun scalarMappingTypeSpec(scalarType: String, javaTypeName: String) =
      TypeSpec.anonymousClassBuilder("")
          .addMethod(MethodSpec.methodBuilder("typeName")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .returns(java.lang.String::class.java)
              .addStatement("return \$S", scalarType)
              .build())
          .addMethod(MethodSpec.methodBuilder("className")
              .addModifiers(Modifier.PUBLIC)
              .apply {
                if (context.suppressRawTypesWarning) {
                  addAnnotation(Annotations.SUPPRESS_RAW_VALUE_WARNING)
                }
              }
              .addAnnotation(Override::class.java)
              .returns(String::class.java)
              .addStatement("return \$S", javaTypeName)
              .build())
          .build()

  companion object {
    fun className(context: CodeGenerationContext): ClassName = ClassName.get(context.ir.typesPackageName, "CustomType")
  }
}
