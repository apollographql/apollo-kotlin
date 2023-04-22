package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.__fields
import com.apollographql.apollo3.compiler.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.apollographql.apollo3.compiler.ir.IrMapProperty
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class InterfaceBuilderBuilder(
    private val context: JavaContext,
    private val iface: IrInterface,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val simpleName = layout.otherBuilderName(iface.name)
  private val mapClassName = ClassName.get(packageName, layout.otherMapName(iface.name))

  override fun prepare() {
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = iface.builderTypeSpec()
    )
  }

  private fun IrInterface.builderTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addField(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .addField(
            FieldSpec.builder(JavaClassNames.MapOfStringToObject, __fields)
                .initializer(CodeBlock.of("new $T<>()", JavaClassNames.HashMap))
                .build()
        )
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JavaClassNames.String, __typename)
                .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
                .addStatement("this.$customScalarAdapters = $customScalarAdapters")
                .addStatement("$__fields.put(\"__typename\", __typename)")
                .build()
        )
        .addMethods(
            this.mapProperties.map {
              it.toMethodSpec()
            }
        )
        .addMethod(
            aliasMethodSpec()
        )
        .addMethod(
            MethodSpec.methodBuilder(Identifier.build)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return new $T($__fields)", mapClassName)
                .returns(mapClassName)
                .build()
        )
        .build()
  }

  private fun aliasMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder("alias")
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get(packageName, simpleName))
        .addParameter(JavaClassNames.String, "alias")
        .addParameter(JavaClassNames.Object, "value")
        .addStatement("$__fields.put(alias, value)")
        .addStatement("return this")
        .build()
  }

  private fun IrMapProperty.toMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(context.layout.propertyName(name))
        .addModifiers(Modifier.PUBLIC)
        .addParameter(context.resolver.resolveIrType2(this.type), context.layout.propertyName(name))
        .returns(ClassName.get(packageName, simpleName))
        .apply {
          val adapter = context.resolver.adapterInitializer2(type)
          val value = if (adapter != null) {
            CodeBlock.of(
                "$T.adaptValue($L, $L)",
                JavaClassNames.ObjectBuilderKt,
                context.resolver.adapterInitializer2(type),
                context.layout.propertyName(name)
            )
          } else {
            CodeBlock.of("$L", context.layout.propertyName(name))
          }

          addStatement(
              "$__fields.put($S, $L)",
              name,
              value
          )
        }
        .addStatement("return this")
        .build()
  }
}
