package com.apollographql.apollo.compiler.backend.ir

import com.apollographql.apollo.compiler.introspection.IntrospectionSchema

internal data class SelectionKey(
    val root: String,
    val keys: List<String>,
    val type: Type,
) {
  operator fun plus(name: String): SelectionKey {
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
    val fragments: List<NamedFragment>,
    val typeDeclarations: List<IntrospectionSchema.TypeRef>,
    val typesPackageName: String,
    val fragmentsPackageName: String,
) {
  data class Operation(
      val name: String,
      val operationName: String,
      val targetPackageName: String,
      val operationType: IntrospectionSchema.TypeRef,
      val comment: String,
      val variables: List<Variable>,
      val definition: String,
      val dataField: Field,
  )

  data class Variable(
      val name: String,
      val type: IntrospectionSchema.TypeRef,
  )

  data class Field(
      val name: String,
      val alias: String?,
      val type: IntrospectionSchema.TypeRef,
      val args: List<Argument>,
      val fields: List<Field>,
      val fragments: List<Fragment>,
      val conditions: List<Condition>,
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

  data class Condition(
      val kind: String,
      val variableName: String,
      val inverted: Boolean,
      val type: Type,
  ) {
    enum class Type {
      Boolean,
    }
  }

  sealed class Fragment {
    abstract val name: String
    abstract val fields: List<Field>
    abstract val possibleTypes: Set<IntrospectionSchema.TypeRef>
    abstract val selectionKeys: Set<SelectionKey>
    abstract val description: String?

    data class Interface(
        override val name: String,
        override val fields: List<Field>,
        override val possibleTypes: Set<IntrospectionSchema.TypeRef>,
        override val selectionKeys: Set<SelectionKey>,
        override val description: String?,
        val typeCondition: IntrospectionSchema.TypeRef,
    ) : Fragment()

    data class Implementation(
        override val name: String,
        override val fields: List<Field>,
        override val possibleTypes: Set<IntrospectionSchema.TypeRef>,
        override val selectionKeys: Set<SelectionKey>,
        override val description: String?,
    ) : Fragment()
  }

  data class NamedFragment(
      val name: String,
      val defaultImplementationName: String,
      val source: String,
      val comment: String,
      val selectionSet: SelectionSet,
      val defaultSelectionSet: SelectionSet,
  ) {
    data class SelectionSet(
        val fields: List<Field>,
        val fragments: List<Fragment>,
        val typeCondition: IntrospectionSchema.TypeRef,
        val possibleTypes: List<IntrospectionSchema.TypeRef>,
        val selectionKeys: Set<SelectionKey>,
    )
  }
}
