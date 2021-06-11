package com.apollographql.apollo3.ast


internal class SchemaValidationScope(document: GQLDocument) : ValidationScope {
  val definitions = document.definitions
  override val issues = mutableListOf<Issue>()

  override val typeDefinitions = getTypeDefinitions(document.definitions)
  override val directives = getDirectives(document.definitions)

  val schemaDefinition = getSchema(document.definitions)

  fun validate(): List<Issue> {
    validateNotExecutable()
    validateNoIntrospectionNames()

    validateInterfaces()
    validateObjects()

    return issues
  }

  private fun validateInterfaces() {
    definitions.filterIsInstance<GQLInterfaceTypeDefinition>().forEach {
      if (it.fields.isEmpty()) {
        registerIssue("Interfaces must specify one or more fields", it.sourceLocation)
      }
    }
  }

  private fun validateObjects() {
    definitions.filterIsInstance<GQLObjectTypeDefinition>().forEach { o ->
      if (o.fields.isEmpty()) {
        registerIssue("Object must specify one or more fields", o.sourceLocation)
      }

      o.implementsInterfaces.forEach { implementsInterface ->
        val iface = definitions.firstOrNull { (it as? GQLInterfaceTypeDefinition)?.name == implementsInterface }
        if (iface == null) {
          registerIssue("Object '${o.name}' cannot implement non-interface '$implementsInterface'", o.sourceLocation)
        }
      }

      o.directives.forEach { directive ->
        validateDirective(directive, GQLDirectiveLocation.OBJECT)
      }
    }
  }

  private fun validateNoIntrospectionNames() {
    // 3.3 All types and directives defined within a schema must not have a name which begins with "__"
    definitions.forEach { definition ->
      if (definition !is GQLNamed) {
        return@forEach
      }
      if (definition.name.startsWith("__")) {
        registerIssue("names starting with '__' are reserved for introspection", definition.sourceLocation)
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
          registerIssue("Found an executable definition. Schemas should not contain operations or fragments.", it.sourceLocation)
        }
  }

  companion object {
    private fun ValidationScope.getTypeDefinitions(definitions: List<GQLDefinition>): Map<String, GQLTypeDefinition> {
      val grouped = definitions.filterIsInstance<GQLTypeDefinition>()
          .groupBy { it.name }

      grouped.values.forEach {
        val first = it.first()
        val occurences = it.map { it.sourceLocation.pretty() }.joinToString("\n")
        if (it.size > 1) {
          issues.add(
              Issue.ValidationError(
                  "type '${first.name}' is defined multiple times:\n$occurences",
                  first.sourceLocation,
                  Issue.Severity.ERROR,
                  ValidationDetails.DuplicateTypeName
              )
          )
        }
      }

      return grouped.mapValues {
        it.value.first()
      }
    }

    private fun ValidationScope.getDirectives(definitions: List<GQLDefinition>): Map<String, GQLDirectiveDefinition> {
      val grouped = definitions.filterIsInstance<GQLDirectiveDefinition>()
          .groupBy { it.name }

      grouped.values.forEach {
        val first = it.first()
        val occurences = it.map { it.sourceLocation.pretty() }.joinToString("\n")
        if (it.size > 1) {
          registerIssue(
              message = "directive '${first.name}' is defined multiple times:\n$occurences",
              sourceLocation = first.sourceLocation,
          )
        }
      }

      return grouped.mapValues {
        it.value.first()
      }
    }


    private fun ValidationScope.getSchema(definitions: List<GQLDefinition>): GQLSchemaDefinition? {
      val schemaDefinitions = definitions.filterIsInstance<GQLSchemaDefinition>()
      if (schemaDefinitions.count() > 1) {
        registerIssue(
            message = "multiple schema definitions found",
            schemaDefinitions.last().sourceLocation,
        )
      }
      return schemaDefinitions.singleOrNull()
    }
  }
}
