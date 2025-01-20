package com.apollographql.apollo.execution

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.Schema


/**
 * An [Instrumentation] monitors the execution algorithm.
 *
 * Compared to a [Resolver], it's also called for built-in introspection fields and can monitor the completed value.
 */
abstract class Instrumentation {
  /**
   * Called before the [Resolver] is called.
   * @return an [FieldCallback] called after the field is executed
   * @throws Exception if something goes wrong. If an instrumentation fails, the whole field
   * fails and an error is returned.
   */
  open fun onOperation(operationInfo: OperationInfo): OperationCallback? {
    return null
  }

  /**
   * Called before the [Resolver] is called.
   * @return an [FieldCallback] called after the field is executed
   * @throws Exception if something goes wrong. If an instrumentation fails, the whole field
   * fails and an error is returned.
   */
  open fun onField(resolveInfo: ResolveInfo): FieldCallback? {
    return null
  }
}

class OperationInfo(
  val operation: GQLOperationDefinition,
  val fragments: Map<String, GQLFragmentDefinition>,
  val schema: Schema,
  val executionContext: ExecutionContext
)

fun interface OperationCallback {
  /**
   * Called when an operation is executed.
   *
   * @param response the response
   * @return a possibly modified response
   */
  fun onOperationCompleted(response: GraphQLResponse): GraphQLResponse
}

fun interface FieldCallback {
  /**
   * Called when a field value is completed.
   *
   * @param value the value after completion
   * @throws Exception if something goes wrong. If an instrumentation fails, the whole field
   * fails and an error is returned.
   */
  fun onFieldCompleted(value: ExternalValue)
}