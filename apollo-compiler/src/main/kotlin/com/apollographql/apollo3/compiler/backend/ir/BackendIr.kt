package com.apollographql.apollo3.compiler.backend.ir

import com.apollographql.apollo3.compiler.introspection.IntrospectionSchema

internal data class SelectionKey(
    val root: String,
    val keys: List<String>,
    val type: Type,
) {
  operator fun plus(name: String): SelectionKey {
    if (name.isEmpty()) return this
    return this.copy(
        keys = this.keys + name
    )
  }

  enum class Type {
    Query, Fragment
  }
}

/**
 * Represents the backend IR after running lowering on parse frontend IR
 *
 * The full chain of lowering process looks like this:
 *
 * GraphQL -->  FrontendIr --> BackendIr -> AST
 *
 * After lowering high-level frontend IR:
 * - GraphQL input / field types are resolved against GraphQL schema
 * - GraphQL variables types are resolved against GraphQL schema
 * - GraphQL fragments lowering by generating fragment interfaces and implementations based on possible types intersection
 * - GraphQL field subselection set is merged with fragment selection set
 * - build selection keys (selection path) for all fields and fragments
 * - etc.
 */
internal data class BackendIr(
    val operations: List<Operation>,
    val fragments: List<NamedFragment>
) {
  data class Operation(
      val name: String,
      val operationName: String,
      val targetPackageName: String,
      val operationSchemaType: IntrospectionSchema.TypeRef,
      val comment: String,
      val variables: List<Variable>,
      val definition: String,
      val dataField: Field,
  )

  data class Variable(
      val name: String,
      val type: IntrospectionSchema.TypeRef,
      val hasDefaultValue: Boolean
  )

  data class Field(
      val name: String,
      val alias: String?,
      val schemaTypeRef: IntrospectionSchema.TypeRef,
      val typeName: String,
      val args: List<Argument>,
      val fields: List<Field>,
      val fragments: Fragments,
      val condition: Condition,
      val description: String,
      val deprecationReason: String?,
      val selectionKeys: Set<SelectionKey>,
  ) {
    val responseName: String = alias ?: name
  }

  data class Argument(
      val name: String,
      val value: Any?,
      val type: IntrospectionSchema.TypeRef,
  )

  sealed class Condition {

    object True : Condition()

    object False : Condition()

    data class Or(val conditions: Set<Condition>) : Condition() {
      init {
        check(conditions.isNotEmpty()) {
          "ApolloGraphQL: cannot create a 'Or' condition from an empty list"
        }
      }
    }

    data class And(val conditions: Set<Condition>) : Condition() {
      init {
        check(conditions.isNotEmpty()) {
          "ApolloGraphQL: cannot create a 'And' condition from an empty list"
        }
      }
    }

    data class Variable(val name: String, val inverted: Boolean) : Condition()
  }

  data class Fragments(
      val fragments: List<Fragment>,
      val accessors: Map<String, SelectionKey>,
  ) : List<Fragment> by fragments

  data class Fragment(
      val name: String,
      val fields: List<Field>,
      val nestedFragments: Fragments?,
      val possibleTypes: Set<IntrospectionSchema.TypeRef>,
      val selectionKeys: Set<SelectionKey>,
      val description: String?,
      val type: Type,
  ) {
    sealed class Type {
      object Interface : Type()
      object Implementation : Type()
      object Fallback : Type()
      data class Delegate(val fragmentSelectionKey: SelectionKey) : Type()
    }
  }

  data class NamedFragment(
      val source: String,
      val comment: String,
      val selectionSet: SelectionSet,
      val implementationSelectionSet: SelectionSet,
      val variables: List<Variable>,
  ) {
    data class SelectionSet(
        val name: String,
        val fields: List<Field>,
        val fragments: Fragments,
        val typeCondition: IntrospectionSchema.TypeRef,
        val possibleTypes: Set<IntrospectionSchema.TypeRef>,
        val defaultSelectionKey: SelectionKey,
        val selectionKeys: Set<SelectionKey>,
    )
  }
}
