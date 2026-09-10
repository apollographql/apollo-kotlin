package com.apollographql.apollo.execution

import com.apollographql.apollo.api.OnError
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLSelection

/**
 * Represents a request just before it is executed.
 *
 * The document is validated and variables are coerced.
 */
class PreparedRequest internal constructor(
    val rootSelections: List<GQLSelection>,
    val typename: String,
    /**
     * Required if [type] is [RequestType.Fragment] to resolve the root object
     */
    val id: String?,
    val fragments: Map<String, GQLFragmentDefinition>,
    val variables: Map<String, InternalValue>,
    val onError: OnError?,
    val type: RequestType,
    val name: String?,
) {
  class Builder {
    private var rootSelections: List<GQLSelection>? = null
    private var typename: String? = null
    private var id: String? = null
    private var fragments: Map<String, GQLFragmentDefinition>? = null
    private var variables: Map<String, InternalValue>? = null
    private var onError: OnError? = null
    private var type: RequestType? = null
    private var name: String? = null

    fun rootSelections(rootSelections: List<GQLSelection>): Builder = apply {
      this.rootSelections = rootSelections
    }

    fun typename(typename: String): Builder = apply {
      this.typename = typename
    }

    /**
     * Required if [type] is [RequestType.Fragment] to resolve the root object
     */
    fun id(id: String?): Builder = apply {
      this.id = id
    }

    fun fragments(fragments: Map<String, GQLFragmentDefinition>): Builder = apply {
      this.fragments = fragments
    }

    fun variables(variables: Map<String, InternalValue>): Builder = apply {
      this.variables = variables
    }

    fun onError(onError: OnError?): Builder = apply {
      this.onError = onError
    }

    fun type(type: RequestType): Builder = apply {
      this.type = type
    }

    fun name(name: String?): Builder = apply {
      this.name = name
    }

    fun build(): PreparedRequest {
      return PreparedRequest(
          rootSelections = checkNotNull(rootSelections) { "rootSelections is required" },
          typename = checkNotNull(typename) { "typename is required" },
          id = id,
          fragments = fragments.orEmpty(),
          variables = variables.orEmpty(),
          onError = onError,
          type = checkNotNull(type) { "type is required" },
          name = name,
      )
    }
  }
}

enum class RequestType {
  Query,
  Mutation,
  Subscription,
  Fragment
}
