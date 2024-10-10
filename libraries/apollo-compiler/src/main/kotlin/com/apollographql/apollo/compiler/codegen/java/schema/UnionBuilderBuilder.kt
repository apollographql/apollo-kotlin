package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.__fields
import com.apollographql.apollo.compiler.codegen.Identifier.__typename
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.typeBuilderPackageName
import com.apollographql.apollo.compiler.ir.IrUnion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class UnionBuilderBuilder(
    private val context: JavaSchemaContext,
    private val union: IrUnion,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typeBuilderPackageName()
  private val simpleName = "Other${layout.schemaTypeName(union.name)}Builder"
  private val mapClassName = ClassName.get(packageName, "Other${layout.schemaTypeName(union.name)}Map")

  override fun prepare() {
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = union.builderTypeSpec()
    )
  }

  private fun IrUnion.builderTypeSpec(): TypeSpec {
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
}
