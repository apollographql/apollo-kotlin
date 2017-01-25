package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.ScalarTypeMapping
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class CustomScalarEnumTypeSpecBuilder(
    val packageName: String,
    val customScalarTypeMap: Map<String, String>
) {
  fun build(): TypeSpec =
      TypeSpec.enumBuilder(ClassName.get(packageName, "CustomScalarType"))
          .addAnnotation(Annotations.GENERATED_BY_APOLLO)
          .addSuperinterface(ScalarTypeMapping::class.java)
          .addModifiers(Modifier.PUBLIC)
          .addEnumConstants()
          .build()

  private fun TypeSpec.Builder.addEnumConstants(): TypeSpec.Builder {
    customScalarTypeMap.forEach { mapping ->
      addEnumConstant(mapping.key.toUpperCase(), scalarMappingTypeSpec(mapping.key, mapping.value))
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
}