package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.rawValue
import com.apollographql.apollo.compiler.codegen.Identifier.safeValueOf
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeSuppressDeprecation
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.internal.escapeJavaReservedWord
import com.apollographql.apollo.compiler.internal.escapeTypeReservedWord
import com.apollographql.apollo.compiler.ir.IrEnum
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class EnumAsClassBuilder(
    private val context: JavaSchemaContext,
    private val enum: IrEnum,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.schemaTypeName(enum.name)

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
        .classBuilder(simpleName)
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
        .addFields(
            values.map { value ->
              FieldSpec.builder(selfClassName, value.targetName.escapeTypeReservedWord() ?: value.targetName.escapeJavaReservedWord())
                  .addModifiers(Modifier.PUBLIC)
                  .addModifiers(Modifier.STATIC)
                  .initializer(CodeBlock.of("new $T($S)", selfClassName, value.name))
                  .build()
            }
        )
        .addMethod(
            MethodSpec.methodBuilder(safeValueOf)
                .addJavadoc(
                    "Returns the ${enum.name} that represents the specified rawValue.\n" +
                        "Note: unknown values of rawValue will return UNKNOWN__. You may want to update your schema instead of calling this method directly.\n",
                )
                .maybeSuppressDeprecation(enum.values)
                .addParameter(JavaClassNames.String, rawValue)
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(selfClassName)
                .addCode(
                    CodeBlock.builder()
                        .beginControlFlow("switch ($T.requireNonNull($rawValue))", JavaClassNames.Objects)
                        .apply {
                          values.forEach {
                            add("case $S: return $T.$L;\n", it.name, selfClassName, it.targetName.escapeTypeReservedWord()
                                ?: it.targetName.escapeJavaReservedWord())
                          }
                        }
                        .add("default: return new $T.${Identifier.UNKNOWN__}($rawValue);\n", selfClassName)
                        .endControlFlow()
                        .build()
                )
                .build()
        )
        .addType(
            unknownTypeSpec()
        )
        .build()
  }

  private fun unknownTypeSpec(): TypeSpec {
    return TypeSpec.classBuilder(Identifier.UNKNOWN__)
        .addModifiers(Modifier.STATIC)
        .addModifiers(Modifier.PUBLIC)
        .superclass(selfClassName)
        .addJavadoc(L, "An enum value that wasn't known at compile time.\n")
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ParameterSpec.builder(JavaClassNames.String, rawValue).build())
                .addCode("super($rawValue);\n")
                .build()
        )
        .addMethod(
            MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JavaClassNames.Override)
                .addParameter(ParameterSpec.builder(JavaClassNames.Object, "other").build())
                .returns(TypeName.BOOLEAN)
                .addCode(
                    CodeBlock.builder()
                        .add("if (this == other) return true;\n")
                        .add("if (!(other instanceof $L)) return false;\n", Identifier.UNKNOWN__)
                        .addStatement("return rawValue.equals((($L) other).rawValue)", Identifier.UNKNOWN__)
                        .build()
                )
                .build()
        )
        .addMethod(
            MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JavaClassNames.Override)
                .returns(TypeName.INT)
                .addCode("return rawValue.hashCode();\n")
                .build()
        )
        .addMethod(
            MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JavaClassNames.Override)
                .returns(JavaClassNames.String)
                .addCode("return \"$L(\" + rawValue + \")\";\n", Identifier.UNKNOWN__)
                .build()
        )
        .build()
  }
}
