package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.ScalarType
import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class CustomEnumTypeSpecBuilder(
    val context: CodeGenerationContext
) {
  fun build(): TypeSpec =
      TypeSpec.enumBuilder(className(context))
          .addAnnotation(Annotations.GENERATED_BY_APOLLO)
          .addSuperinterface(ScalarType::class.java)
          .addModifiers(Modifier.PUBLIC)
          .addEnumConstants()
          .build()

  private fun TypeSpec.Builder.addEnumConstants(): TypeSpec.Builder {
    context.customTypeMap.forEach { mapping ->
      val constantName = mapping.key.removeSuffix("!").toUpperCase()
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
          .addMethod(MethodSpec.methodBuilder("javaType")
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .returns(Class::class.java)
              .addStatement("return \$T.class", javaTypeName.toJavaType())
              .build())
          .build()

  companion object {
    fun className(context: CodeGenerationContext): ClassName = ClassName.get(context.typesPackage, "CustomType")
  }

}