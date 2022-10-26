package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.rawValue
import com.apollographql.apollo3.compiler.codegen.Identifier.safeValueOf
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeSuppressDeprecation
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class EnumAsEnumBuilder(
    private val context: JavaContext,
    private val enum: IrEnum,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.enumName(enum.name)

  private val selfClassName: ClassName
    get() = context.resolver.resolveSchemaType(enum.name)

  override fun prepare() {
    context.resolver.registerSchemaType(
        enum.name,
        ClassName.get(packageName, simpleName)
    )
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = enum.typeSpec()
    )
  }

  private fun IrEnum.typeSpec(): TypeSpec {
    return TypeSpec
        .enumBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .maybeAddDescription(description)
        .addField(typeFieldSpec())
        .addField(
            FieldSpec.builder(JavaClassNames.String, rawValue)
                .addModifiers(Modifier.PUBLIC)
                .build()
        )
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ParameterSpec.builder(JavaClassNames.String, rawValue).build())
                .addCode("this.$rawValue = $rawValue;\n")
                .build()
        )
        .apply {
          values.forEach { value ->
            addEnumConstant(layout.enumValueName(value.targetName), value.enumConstTypeSpec())
          }
          addEnumConstant("UNKNOWN__", unknownValueTypeSpec())

        }
        .addMethod(
            MethodSpec.methodBuilder(safeValueOf)
                .maybeSuppressDeprecation(enum.values)
                .addParameter(JavaClassNames.String, rawValue)
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(selfClassName)
                .addCode(
                    CodeBlock.builder()
                        .beginControlFlow("switch ($rawValue)")
                        .apply {
                          values.forEach {
                            add("case $S: return $T.$L;\n", it.name, selfClassName, layout.enumValueName(it.targetName))
                          }
                        }
                        .add("default: return $T.${Identifier.UNKNOWN__};\n", selfClassName)
                        .endControlFlow()
                        .build()
                )
                .build()
        )
        .build()
  }

  private fun IrEnum.Value.enumConstTypeSpec(): TypeSpec {
    return TypeSpec.anonymousClassBuilder(S, name)
        .maybeAddDeprecation(deprecationReason)
        .maybeAddDescription(description)
        .build()
  }

  private fun unknownValueTypeSpec(): TypeSpec {
    return TypeSpec.anonymousClassBuilder(S, Identifier.UNKNOWN__)
        .addJavadoc(L, "Auto generated constant for unknown enum values\n")
        .build()
  }
}
