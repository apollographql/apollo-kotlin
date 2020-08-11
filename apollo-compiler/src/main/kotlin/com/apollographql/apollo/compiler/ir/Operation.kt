package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.compiler.withBuilder
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class Operation(
    val operationName: String,
    val packageName: String,
    val operationType: String,
    val description: String,
    val variables: List<Variable>,
    val source: String,
    val sourceWithFragments: String,
    val fields: List<Field>,
    val filePath: String,
    val fragments: List<FragmentRef>,
    val fragmentsReferenced: List<String>
) : CodeGenerator {

  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec =
      SchemaTypeSpecBuilder(
          typeName = DATA_TYPE_NAME,
          description = "Data from the response after executing this GraphQL operation",
          fields = fields,
          fragments = fragments,
          inlineFragments = emptyList(),
          context = context,
          abstract = abstract
      )
          .build(Modifier.PUBLIC, Modifier.STATIC)
          .toBuilder()
          .addSuperinterface(Operation.Data::class.java)
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

  companion object {
    const val DATA_TYPE_NAME = "Data"
    const val TYPE_MUTATION = "mutation"
    const val TYPE_QUERY = "query"
    const val TYPE_SUBSCRIPTION = "subscription"
  }
}
