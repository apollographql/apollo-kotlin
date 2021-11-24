/*
 * Generates ResponseAdapters for variables/input
 */
package com.apollographql.apollo3.compiler.codegen.java.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.java.helpers.writeToResponseCodeBlock
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
      .addSuperinterface(ParameterizedTypeName.get(JavaClassNames.Adapter, adaptedTypeName))
      .addMethod(notImplementedFromResponseMethodSpec(adaptedTypeName))
      .addMethod(writeToResponseMethodSpec(context, adaptedTypeName))
      .build()
}

private fun notImplementedFromResponseMethodSpec(adaptedTypeName: TypeName) = MethodSpec.methodBuilder(fromJson)
    .addModifiers(Modifier.PUBLIC)
    .addException(JavaClassNames.IOException)
    .addAnnotation(JavaClassNames.Override)
    .addParameter(JavaClassNames.JsonReader, Identifier.reader)
    .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
    .returns(adaptedTypeName)
    .addCode("throw new $T($S);\n", JavaClassNames.IllegalStateException, "Input type used in output position")
    .build()


private fun List<NamedType>.writeToResponseMethodSpec(
    context: JavaContext,
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


