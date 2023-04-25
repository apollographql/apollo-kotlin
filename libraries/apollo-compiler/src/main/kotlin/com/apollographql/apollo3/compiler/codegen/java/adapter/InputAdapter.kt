/*
 * Generates ResponseAdapters for input
 */
package com.apollographql.apollo3.compiler.codegen.java.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.Empty
import com.apollographql.apollo3.compiler.codegen.Identifier.deserializeData
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeData
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.java.helpers.beginOptionalControlFlow
import com.apollographql.apollo3.compiler.codegen.java.helpers.suppressDeprecatedAnnotation
import com.apollographql.apollo3.compiler.ir.isOptional
import com.apollographql.apollo3.compiler.ir.isScalarOrWrappedScalar
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier


internal fun List<NamedType>.inputAdapterTypeSpec(
    context: JavaContext,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.enumBuilder(adapterName)
      .addModifiers(Modifier.PUBLIC)
      .addEnumConstant("INSTANCE")
      .addSuperinterface(ParameterizedTypeName.get(JavaClassNames.DataAdapter, adaptedTypeName))
      .addMethod(notImplementedFromResponseMethodSpec(adaptedTypeName))
      .addMethod(writeToResponseMethodSpec(context, adaptedTypeName))
      .apply {
        if (this@inputAdapterTypeSpec.any { it.deprecationReason != null }) {
          addAnnotation(suppressDeprecatedAnnotation())
        }
      }
      .build()
}

private fun notImplementedFromResponseMethodSpec(adaptedTypeName: TypeName) = MethodSpec.methodBuilder(deserializeData)
    .addModifiers(Modifier.PUBLIC)
    .addException(JavaClassNames.IOException)
    .addAnnotation(JavaClassNames.Override)
    .addParameter(JavaClassNames.JsonReader, Identifier.reader)
    .addParameter(JavaClassNames.DeserializeDataContext, Identifier.context)
    .returns(adaptedTypeName)
    .addCode("throw new $T($S);\n", JavaClassNames.IllegalStateException, "Input type used in output position")
    .build()


private fun List<NamedType>.writeToResponseMethodSpec(
    context: JavaContext,
    adaptedTypeName: TypeName,
): MethodSpec {
  return MethodSpec.methodBuilder(serializeData)
      .addModifiers(Modifier.PUBLIC)
      .addException(JavaClassNames.IOException)
      .addAnnotation(JavaClassNames.Override)
      .addParameter(JavaClassNames.JsonWriter, writer)
      .addParameter(adaptedTypeName, value)
      .addParameter(JavaClassNames.SerializeDataContext, Identifier.context)
      .addCode(writeToResponseCodeBlock(context))
      .build()
}

private fun List<NamedType>.writeToResponseCodeBlock(context: JavaContext): CodeBlock {
  val builder = CodeBlock.builder()
  forEach {
    builder.add(it.writeToResponseCodeBlock(context))
  }
  return builder.build()
}

private fun NamedType.writeToResponseCodeBlock(context: JavaContext): CodeBlock {
  val adapterInitializer = context.resolver.adapterInitializer(type, false)
  val builder = CodeBlock.builder()
  val propertyName = context.layout.propertyName(graphQlName)

  if (type.isOptional()) {
    builder.beginOptionalControlFlow(propertyName, context.nullableFieldStyle)
  }
  builder.add("$writer.name($S);\n", graphQlName)
  if (type.isScalarOrWrappedScalar()) {
    builder.addStatement("$L.$toJson($writer, $T.$Empty, $value.$propertyName)", adapterInitializer, JavaClassNames.CustomScalarAdapters)
  } else {
    builder.addStatement("$L.$serializeData($writer, $value.$propertyName, ${Identifier.context})", adapterInitializer)
  }
  if (type.isOptional()) {
    builder.endControlFlow()
  }
  return builder.build()
}
