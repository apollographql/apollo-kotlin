package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.rawValue
import com.apollographql.apollo.compiler.codegen.Identifier.reader
import com.apollographql.apollo.compiler.codegen.Identifier.safeValueOf
import com.apollographql.apollo.compiler.codegen.Identifier.toJson
import com.apollographql.apollo.compiler.codegen.Identifier.value
import com.apollographql.apollo.compiler.codegen.Identifier.writer
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.responseAdapter
import com.apollographql.apollo.compiler.codegen.typeAdapterPackageName
import com.apollographql.apollo.compiler.ir.IrEnum
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class EnumResponseAdapterBuilder(
    val context: JavaSchemaContext,
    val enum: IrEnum,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typeAdapterPackageName()
  private val simpleName = layout.schemaTypeName(enum.name).responseAdapter()

  override fun prepare() {
    context.resolver.registerEnumAdapter(
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
    val adaptedTypeName = context.resolver.resolveSchemaType(enum.name)
    val fromResponseMethodSpec = MethodSpec.methodBuilder(Identifier.fromJson)
        .addModifiers(Modifier.PUBLIC)
        .addException(JavaClassNames.IOException)
        .addAnnotation(JavaClassNames.Override)
        .addParameter(JavaClassNames.JsonReader, reader)
        .addParameter(JavaClassNames.CustomScalarAdapters, Identifier.customScalarAdapters)
        .returns(adaptedTypeName)
        .addCode(
            CodeBlock.builder()
                .addStatement("String $rawValue = reader.nextString()")
                .addStatement("return $T.$safeValueOf($rawValue)", adaptedTypeName)
                .build()
        )
        .build()
    val toResponseMethodSpec = toResponseMethodSpecBuilder(adaptedTypeName)
        .addCode("$writer.$value($value.rawValue);\n")
        .build()

    return TypeSpec.enumBuilder(layout.schemaTypeName(name).responseAdapter())
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstant("INSTANCE")
        .addSuperinterface(ParameterizedTypeName.get(JavaClassNames.Adapter, adaptedTypeName))
        .addMethod(fromResponseMethodSpec)
        .addMethod(toResponseMethodSpec)
        .build()
  }
}

internal fun toResponseMethodSpecBuilder(typeName: TypeName) = MethodSpec.methodBuilder(toJson)
    .addModifiers(Modifier.PUBLIC)
    .addException(JavaClassNames.IOException)
    .addAnnotation(JavaClassNames.Override)
    .addParameter(JavaClassNames.JsonWriter, writer)
    .addParameter(JavaClassNames.CustomScalarAdapters, Identifier.customScalarAdapters)
    .addParameter(typeName, value)
