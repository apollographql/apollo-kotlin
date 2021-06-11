package com.apollographql.apollo3.ast


internal class SchemaValidationScope(private val document: GQLDocument) {
  val definitions = document.definitions
  private val issues = mutableListOf<Issue>()

  val typeDefinitions = getTypeDefinitions(document.definitions)
  val directives = getDirectives(document.definitions)

  fun validate(): List<Issue> {
    validateNotExecutable()
    validateUniqueSchemaDefinition()
    validateNoIntrospectionNames()

    validateInterfaces()
    validateObjects()

    return issues
  }

  fun validateInterfaces() {
    definitions.filterIsInstance<GQLInterfaceTypeDefinition>().forEach {
      if (it.fields.isEmpty()) {
        issues.add(Issue.ValidationError("Interfaces must specify one or more fields", it.sourceLocation))
      }
    }
  }

  private fun validateObjects() {
    definitions.filterIsInstance<GQLObjectTypeDefinition>().forEach { o ->
      if (o.fields.isEmpty()) {
        issues.add(Issue.ValidationError("Object must specify one or more fields", o.sourceLocation))
      }

      o.implementsInterfaces.forEach { implementsInterface ->
        val iface = definitions.firstOrNull { (it as? GQLInterfaceTypeDefinition)?.name == implementsInterface }
        if (iface == null) {
          issues.add(Issue.ValidationError("Object '${o.name}' cannot implement non-interface '$implementsInterface'", o.sourceLocation))
        }
      }

      o.directives.forEach { directive ->
        val directiveDefinition = directives[directive.name]
        if (directiveDefinition == null) {
          issues.add(Issue.ValidationError("Object '${o.name}' cannot implement non-interface '$implementsInterface'", o.sourceLocation))
        }
        directive.validate(directiveDefinition)
      }
    }
  }

  private fun validateUniqueSchemaDefinition() {
    val schemaDefinitions = definitions.filterIsInstance<GQLSchemaDefinition>()
    if (schemaDefinitions.count() > 1) {
      issues.add(Issue.ValidationError("multiple schema definitions found", schemaDefinitions.last().sourceLocation))
    }
  }

  private fun validateNoIntrospectionNames() {
    // 3.3 All types and directives defined within a schema must not have a name which begins with "__"
    definitions.forEach { definition ->
      if (definition !is GQLNamed) {
        return@forEach
      }
      if (definition.name.startsWith("__")) {
        issues.add(Issue.ValidationError("names starting with '__' are reserved for introspection", definition.sourceLocation))
      }
    }
  }

  /**
   * This is not in the specification per-se but in our use case, that will help catch some cases when users mistake
   * graphql operations for schemas
   */
  private fun validateNotExecutable() {
    definitions.firstOrNull { it is GQLOperationDefinition || it is GQLFragmentDefinition }
        ?.let {
          issues.add(Issue.ValidationError("Found an executable definition. Schemas should not contain operations or fragments.", it.sourceLocation))
        }
  }

  companion object {
    fun getTypeDefinitions(definitions: List<GQLDefinition>): Map<String, GQLTypeDefinition> {
      val grouped = definitions.filterIsInstance<GQLTypeDefinition>()
          .groupBy { it.name }

      grouped.values.forEach {
        val first = it.first()
        val occurences = it.map { it.sourceLocation.pretty() }.joinToString("\n")
        if (it.size > 1) {
          Issue.ValidationError(
              "type '${first.name}' is defined multiple times:\n$occurences",
              first.sourceLocation,
              ValidationDetails.DuplicateTypeName
          )
        }
      }

      return grouped.mapValues {
        it.value.first()
      }
    }

    fun getDirectives(definitions: List<GQLDefinition>): Map<String, GQLDirectiveDefinition> {
      val grouped = definitions.filterIsInstance<GQLDirectiveDefinition>()
          .groupBy { it.name }

      grouped.values.forEach {
        val first = it.first()
        val occurences = it.map { it.sourceLocation.pretty() }.joinToString("\n")
        if (it.size > 1) {
          Issue.ValidationError(
              "directive '${first.name}' is defined multiple times:\n$occurences",
              first.sourceLocation,
          )
        }
      }

      return grouped.mapValues {
        it.value.first()
      }
    }
  }
}
