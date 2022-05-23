package com.apollographql.apollo3.ast

import com.apollographql.apollo3.ast.internal.buffer

// 5.5.2.3 Fragment spread is possible
internal fun GQLTypeDefinition.sharesPossibleTypesWith(other: GQLTypeDefinition, schema: Schema): Boolean {
  return schema.possibleTypes(this).intersect(schema.possibleTypes(other)).isNotEmpty()
}

fun GQLTypeDefinition.possibleTypes(schema: Schema): Set<String> {
  return schema.possibleTypes(this)
}

fun GQLTypeDefinition.isFieldNonNull(fieldName: String): Boolean {
  val directive = when (this) {
    is GQLObjectTypeDefinition -> directives
    is GQLInterfaceTypeDefinition -> directives
    else -> return false
  }.firstOrNull { it.name == "nonnull" }

  if (directive == null) {
    return false
  }

  val stringValue = (directive.arguments!!.arguments.first().value as GQLStringValue).value

  val selections = stringValue.buffer().parseAsGQLSelections().valueAssertNoErrors()

  return selections.filterIsInstance<GQLField>()
      .map { it.name }
      .contains(fieldName)
}

fun GQLTypeDefinition.isAbstract(): Boolean {
  return when (this) {
    is GQLUnionTypeDefinition,
    is GQLInterfaceTypeDefinition,
    -> true
    else -> false
  }
}
fun GQLTypeDefinition.implementsAbstractType(schema: Schema): Boolean {
  return schema.implementedTypes(name).any {
    schema.typeDefinition(it).isAbstract()
  }
}

fun GQLTypeDefinition.canHaveKeyFields(): Boolean {
  return when (this) {
    is GQLObjectTypeDefinition,
    is GQLInterfaceTypeDefinition,
    is GQLUnionTypeDefinition
    -> true
    else -> false
  }
}

/**
 * Get the key fields for an object, interface or union type.
 *
 * If this type has one or multiple @[Schema.TYPE_POLICY] annotation(s), they are used, else it recurses in implemented interfaces until it
 * finds some.
 *
 * @param typeDefinitions a list of all the type definitions that the type may implement
 *
 * @return The names of the key fields declared on the type and anything it implements, or the emptySet if the type has no key fields.
 */
fun GQLTypeDefinition.keyFields(typeDefinitions: Map<String, GQLTypeDefinition>): Set<String> {
  return when (this) {
    is GQLObjectTypeDefinition -> {
      val kf = directives.toKeyFields()
      if (kf != null) {
        kf
      } else {
        val kfs = implementsInterfaces.map { it to typeDefinitions[it]!!.keyFields(typeDefinitions) }.filter { it.second.isNotEmpty() }
        if (kfs.isNotEmpty()) {
          check(kfs.size == 1) {
            val candidates = kfs.joinToString("\n") { "${it.first}: ${it.second}" }
            "Object '${name}' cannot inherit different keys from different interfaces:\n$candidates"
          }
        }
        kfs.singleOrNull()?.second ?: emptySet()
      }
    }
    is GQLInterfaceTypeDefinition -> {
      val kf = directives.toKeyFields()
      if (kf != null) {
        kf
      } else {
        val kfs = implementsInterfaces.map { it to typeDefinitions[it]!!.keyFields(typeDefinitions) }.filter { it.second.isNotEmpty() }
        if (kfs.isNotEmpty()) {
          check(kfs.size == 1) {
            val candidates = kfs.joinToString("\n") { "${it.first}: ${it.second}" }
            "Interface '$name' cannot inherit different keys from different interfaces:\n$candidates"
          }
        }
        kfs.singleOrNull()?.second ?: emptySet()
      }
    }
    is GQLUnionTypeDefinition -> directives.toKeyFields() ?: emptySet()
    else -> emptySet() // If we come here, the schema is invalid and this should be caught by other validation rules
  }
}

private fun List<GQLDirective>.toKeyFields(): Set<String>? {
  val directives = filter { it.name == Schema.TYPE_POLICY }
  if (directives.isEmpty()) {
    return null
  }
  return directives.flatMap {
    (it.arguments!!.arguments.first().value as GQLStringValue).value.buffer().parseAsGQLSelections().valueAssertNoErrors().map { gqlSelection ->
      // No need to check here, this should be done during validation
      (gqlSelection as GQLField).name
    }
  }.toSet()
}