package com.apollographql.apollo.compiler.codegen.java.adapters

import com.apollographql.apollo.compiler.JavaNullable
import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo.compiler.codegen.Identifier.reader
import com.apollographql.apollo.compiler.codegen.Identifier.value
import com.apollographql.apollo.compiler.codegen.Identifier.writer
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.javaOptionalAdapterClassName
import com.apollographql.apollo.compiler.codegen.typeAdapterPackageName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import javax.lang.model.element.Modifier

internal class JavaOptionalAdapterBuilder(
    private val context: JavaSchemaContext,
    private val nullableFieldStyle: JavaNullable,
) : JavaClassBuilder {
  private val packageName = context.layout.typeAdapterPackageName()
  private val simpleName = context.layout.javaOptionalAdapterClassName()

  override fun prepare() {
    context.resolver.registerJavaOptionalAdapter(ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = javaOptionalAdapterTypeSpec()
    )
  }

  private fun javaOptionalAdapterTypeSpec(): TypeSpec {
    val t = TypeVariableName.get("T")
    val javaOptional = when (nullableFieldStyle) {
      JavaNullable.JAVA_OPTIONAL -> JavaClassNames.JavaOptional
      JavaNullable.GUAVA_OPTIONAL -> JavaClassNames.GuavaOptional
      else -> error("Unsupported nullableFieldStyle: $nullableFieldStyle")
    }
    val javaOptionalT = ParameterizedTypeName.get(javaOptional, t)
    val adapterT = ParameterizedTypeName.get(JavaClassNames.Adapter, t)
    val adapterOptionalT = ParameterizedTypeName.get(JavaClassNames.Adapter, javaOptionalT)

    val wrappedAdapterField = FieldSpec.builder(adapterT, "wrappedAdapter", Modifier.PRIVATE, Modifier.FINAL).build()

    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(adapterT, "wrappedAdapter")
        .addStatement("this.wrappedAdapter = wrappedAdapter")
        .build()

    val absentFunName = when (nullableFieldStyle) {
      JavaNullable.JAVA_OPTIONAL -> "empty"
      JavaNullable.GUAVA_OPTIONAL -> "absent"
      else -> error("Unsupported nullableFieldStyle: $nullableFieldStyle")
    }
    val fromJson = MethodSpec.methodBuilder(fromJson)
        .addException(JavaClassNames.IOException)
        .addAnnotation(JavaClassNames.Override)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(JavaClassNames.JsonReader, reader)
        .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .returns(javaOptionalT)
        .beginControlFlow("if ($reader.peek() == $T.Token.NULL)", JavaClassNames.JsonReader)
        .addStatement("$reader.skipValue()")
        .addStatement("return $T.$absentFunName()", javaOptional)
        .endControlFlow()
        .addStatement("return $T.of(wrappedAdapter.fromJson($reader, $customScalarAdapters))", javaOptional)
        .build()

    val toJson = MethodSpec.methodBuilder(Identifier.toJson)
        .addException(JavaClassNames.IOException)
        .addAnnotation(JavaClassNames.Override)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(JavaClassNames.JsonWriter, writer)
        .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .addParameter(javaOptionalT, value)
        .beginControlFlow("if (!$value.isPresent())")
        .addStatement("$writer.nullValue()")
        .nextControlFlow("else")
        .addStatement("wrappedAdapter.toJson($writer, $customScalarAdapters, $value.get())")
        .endControlFlow()
        .build()

    return TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariable(t)
        .addSuperinterface(adapterOptionalT)
        .addField(wrappedAdapterField)
        .addMethod(constructor)
        .addMethod(fromJson)
        .addMethod(toJson)
        .build()
  }
}
