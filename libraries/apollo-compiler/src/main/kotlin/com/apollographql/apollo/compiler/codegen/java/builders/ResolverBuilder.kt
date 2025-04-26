package com.apollographql.apollo.compiler.codegen.java.builders

import com.apollographql.apollo.compiler.codegen.builderResolverPackageName
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaDataBuilderContext
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class ResolverBuilder(
    private val context: JavaDataBuilderContext,
    private val possibleTypes: Map<String, List<String>>,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.builderResolverPackageName()
  private val simpleName = "DefaultFakeResolver"

  override fun prepare() {
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typeSpec(): TypeSpec {
    val mapTypeName = ParameterizedTypeName.get(JavaClassNames.Map, JavaClassNames.String, ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.String))
    return TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .superclass(JavaClassNames.BaseFakeResolver)
        .addField(
            FieldSpec.builder(mapTypeName, "possibleTypes")
            .addModifiers(Modifier.STATIC, Modifier.PRIVATE)
                .initializer("new $T<>()", JavaClassNames.HashMap)
            .build()
        )
        .addStaticBlock(
            CodeBlock.builder()
                .apply {
                  possibleTypes.entries.forEach {
                    add("possibleTypes.put($S, $T.asList(", it.key, JavaClassNames.Arrays)
                    var isFirst = true
                    it.value.forEach {
                      if (!isFirst) {
                        add(",")
                      }
                      isFirst = false
                      add("$S", it)
                    }
                    add("));\n")
                  }
                }
                .build()
        )
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addCode("super(possibleTypes);\n")
                .build()
        )
        .build()
  }
}