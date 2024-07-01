/*
 * Generates ResponseAdapters for input
 */
package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo.compiler.codegen.Identifier.reader
import com.apollographql.apollo.compiler.codegen.Identifier.toJson
import com.apollographql.apollo.compiler.codegen.Identifier.value
import com.apollographql.apollo.compiler.codegen.Identifier.writer
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaContext
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.javaPropertyName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier


internal fun List<NamedType>.inputAdapterTypeSpec(
    context: JavaSchemaContext,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.enumBuilder(adapterName)
      .addModifiers(Modifier.PUBLIC)
      .addEnumConstant("INSTANCE")
      .addSuperinterface(ParameterizedTypeName.get(JavaClassNames.Adapter, adaptedTypeName))
      .addMethod(notImplementedFromResponseMethodSpec(adaptedTypeName))
      .addMethod(writeToResponseMethodSpec(context, adaptedTypeName))
      .apply {
        if (this@inputAdapterTypeSpec.any { it.deprecationReason != null }) {
          addAnnotation(suppressDeprecatedAnnotation())
        }
      }
      .build()
}

private fun notImplementedFromResponseMethodSpec(adaptedTypeName: TypeName) = MethodSpec.methodBuilder(fromJson)
    .addModifiers(Modifier.PUBLIC)
    .addException(JavaClassNames.IOException)
    .addAnnotation(JavaClassNames.Override)
    .addParameter(JavaClassNames.JsonReader, reader)
    .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
    .returns(adaptedTypeName)
    .addCode("throw new $T($S);\n", JavaClassNames.IllegalStateException, "Input type used in output position")
    .build()


private fun List<NamedType>.writeToResponseMethodSpec(
    context: JavaSchemaContext,
    adaptedTypeName: TypeName,
): MethodSpec {
  return MethodSpec.methodBuilder(toJson)
      .addModifiers(Modifier.PUBLIC)
      .addException(JavaClassNames.IOException)
      .addAnnotation(JavaClassNames.Override)
      .addParameter(JavaClassNames.JsonWriter, writer)
      .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
      .addParameter(adaptedTypeName, value)
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
  val propertyName = context.layout.javaPropertyName(graphQlName)

  if (type.optional) {
    builder.beginOptionalControlFlow(propertyName, context.nullableFieldStyle)
  }
  builder.add("$writer.name($S);\n", graphQlName)
  builder.addStatement("$L.$toJson($writer, $customScalarAdapters, $value.$propertyName)", adapterInitializer)

  if (type.optional) {
    builder.endControlFlow()
  }
  return builder.build()
}
