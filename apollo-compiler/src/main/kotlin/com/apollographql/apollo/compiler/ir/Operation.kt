package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.internal.SimpleResponseWriter
import com.apollographql.apollo.compiler.Annotations
import com.apollographql.apollo.compiler.ClassNames
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.apollographql.apollo.compiler.withBuilder
import com.apollographql.apollo.response.ScalarTypeAdapters
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import javax.lang.model.element.Modifier

data class Operation(
    val operationName: String,
    val operationType: String,
    val variables: List<Variable>,
    val source: String,
    val sourceWithFragments: String,
    val fields: List<Field>,
    val filePath: String,
    val fragmentsReferenced: List<String>
) : CodeGenerator {

  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec =
      SchemaTypeSpecBuilder(
          typeName = DATA_TYPE_NAME,
          fields = fields,
          fragmentRefs = emptyList(),
          inlineFragments = emptyList(),
          context = context,
          abstract = abstract
      )
          .build(Modifier.PUBLIC, Modifier.STATIC)
          .toBuilder()
          .addSuperinterface(Operation.Data::class.java)
          .addMethod(toJsonWithDefaultScalarTypesMethodSpec())
          .addMethod(toJsonMethodSpec())
          .build()
          .let {
            if (context.generateModelBuilder) {
              it.withBuilder()
            } else {
              it
            }
          }

  fun normalizedOperationName(useSemanticNaming: Boolean): String = when (operationType) {
    TYPE_MUTATION -> normalizedOperationName(useSemanticNaming, "Mutation")
    TYPE_QUERY -> normalizedOperationName(useSemanticNaming, "Query")
    TYPE_SUBSCRIPTION -> normalizedOperationName(useSemanticNaming, "Subscription")
    else -> throw IllegalArgumentException("Unknown operation type $operationType")
  }

  private fun normalizedOperationName(useSemanticNaming: Boolean, operationNameSuffix: String): String {
    return if (useSemanticNaming && !operationName.endsWith(operationNameSuffix)) {
      operationName.capitalize() + operationNameSuffix
    } else {
      operationName.capitalize()
    }
  }

  fun isMutation() = operationType == TYPE_MUTATION

  fun isQuery() = operationType == TYPE_QUERY

  fun isSubscription() = operationType == TYPE_SUBSCRIPTION

  private fun toJsonWithDefaultScalarTypesMethodSpec(): MethodSpec {
    return MethodSpec
        .methodBuilder("toJson")
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(String::class.java)
        .addParameter(ParameterSpec
            .builder(ClassNames.STRING, "indent")
            .addAnnotation(Annotations.NONNULL)
            .build()
        )
        .addStatement("return toJson(indent, \$T.DEFAULT)", ScalarTypeAdapters::class.java)
        .build()
  }

  private fun toJsonMethodSpec(): MethodSpec {
    return MethodSpec
        .methodBuilder("toJson")
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(String::class.java)
        .addParameter(ParameterSpec
            .builder(ClassNames.STRING, "indent")
            .addAnnotation(Annotations.NONNULL)
            .build()
        )
        .addParameter(ParameterSpec
            .builder(ScalarTypeAdapters::class.java, "scalarTypeAdapters")
            .addAnnotation(Annotations.NONNULL)
            .build()
        )
        .addCode("""
            try {
              final ${'$'}T responseWriter = new ${'$'}T(scalarTypeAdapters);
              marshaller().marshal(responseWriter);
              return responseWriter.toJson(indent);
            } catch (${'$'}T e) {
              throw new ${'$'}T(e);
            }
           
        """.trimIndent(), SimpleResponseWriter::class.java, SimpleResponseWriter::class.java, IOException::class.java,
            RuntimeException::class.java)
        .build()
  }

  companion object {
    const val DATA_TYPE_NAME = "Data"
    const val TYPE_MUTATION = "mutation"
    const val TYPE_QUERY = "query"
    const val TYPE_SUBSCRIPTION = "subscription"
  }
}
