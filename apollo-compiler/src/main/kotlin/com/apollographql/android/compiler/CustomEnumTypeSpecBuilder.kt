package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.TypeMapping
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
          .addSuperinterface(TypeMapping::class.java)
          .addModifiers(Modifier.PUBLIC)
          .addEnumConstants()
          .build()

  private fun TypeSpec.Builder.addEnumConstants(): TypeSpec.Builder {
    context.customTypeMap.forEach { mapping ->
      val constantName = mapping.key.removeSuffix("!").toUpperCase()
      addEnumConstant(constantName, scalarMappingTypeSpec(mapping.key, mapping.value))
    }
    return this
  }

  private fun scalarMappingTypeSpec(scalarType: String, javaClassName: String) =
      TypeSpec.anonymousClassBuilder("")
          .addMethod(MethodSpec.methodBuilder("type")
              .addModifiers(Modifier.PUBLIC)
              .returns(java.lang.String::class.java)
              .addStatement("return \$S", scalarType)
              .build())
          .addMethod(MethodSpec.methodBuilder("clazz")
              .addModifiers(Modifier.PUBLIC)
              .returns(Class::class.java)
              .addStatement("return \$T.class", ClassName.get(javaClassName.substringBeforeLast("."),
                  javaClassName.substringAfterLast(".")))
              .build())
          .build()

  companion object {
    fun className(context: CodeGenerationContext): ClassName = ClassName.get(context.typesPackage, "CustomType")
  }

}