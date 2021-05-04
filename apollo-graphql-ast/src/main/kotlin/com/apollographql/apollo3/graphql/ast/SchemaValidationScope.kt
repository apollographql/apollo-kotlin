package com.apollographql.apollo3.graphql.ast



internal class SchemaValidationScope {
  private val issues = mutableListOf<Issue>()

  fun validate(document: GQLDocument): List<Issue> {
    return document.validateAsSchema()
  }

  private fun GQLDocument.validateAsSchema(): List<Issue> {
    validateNotExecutable()
    validateUniqueSchemaDefinition()
    validateTypeNames()
    validateDirectiveNames()
    validateInterfaces()
    validateObjects()

    return issues
  }

  private fun GQLDocument.validateInterfaces() {
    definitions.filterIsInstance<GQLInterfaceTypeDefinition>().forEach {
      if (it.fields.isEmpty()) {
        issues.add(Issue.ValidationError("Interfaces must specify one or more fields", it.sourceLocation))
      }
    }
  }

  private fun GQLDocument.validateObjects() {
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
    }
  }

  private fun GQLDocument.validateUniqueSchemaDefinition() {
    val schemaDefinitions = definitions.filter { it is GQLSchemaDefinition }
    if (schemaDefinitions.count() > 1) {
      issues.add(Issue.ValidationError("multiple schema definitions found", schemaDefinitions.last().sourceLocation))
    }
  }

  private fun GQLDocument.validateTypeNames() {
    val typeDefinitions = mutableMapOf<String, GQLTypeDefinition>()
    val conflicts = mutableListOf<GQLTypeDefinition>()
    definitions.filterIsInstance<GQLTypeDefinition>().forEach {
      val name = it.name

      if (!typeDefinitions.containsKey(name)) {
        typeDefinitions[name] = it
      } else {
        conflicts.add(it)
      }
    }

    // 3.3 All types within a GraphQL schema must have unique names
    if (conflicts.size > 0) {
      val conflict = conflicts.first()
      issues.add(Issue.ValidationError("type '${conflict.name}' is defined multiple times", conflict.sourceLocation))
    }

    // 3.3 All types and directives defined within a schema must not have a name which begins with "__"
    typeDefinitions.forEach { name, definition ->
      if (name.startsWith("__")) {
        issues.add(Issue.ValidationError("names starting with '__' are reserved for introspection", definition.sourceLocation))
      }
    }
  }

  private fun GQLDocument.validateDirectiveNames() {
    val directiveDefinitions = mutableMapOf<String, GQLDirectiveDefinition>()
    val conflicts = mutableListOf<GQLDirectiveDefinition>()
    definitions
        .filterIsInstance<GQLDirectiveDefinition>()
        .forEach {
          val name = it.name

          if (!directiveDefinitions.containsKey(name)) {
            directiveDefinitions.put(name, it)
          } else {
            conflicts.add(it)
          }
        }

    // 3.3 All directives within a GraphQL schema must have unique names.
    if (conflicts.size > 0) {
      val conflict = conflicts.first()
      issues.add(Issue.ValidationError("directive '${conflict.name}' is defined multiple times", conflict.sourceLocation))
    }

    // 3.3 All types and directives defined within a schema must not have a name which begins with "__"
    directiveDefinitions.forEach { name, definition ->
      if (name.startsWith("__")) {
        issues.add(Issue.ValidationError("names starting with '__' are reserved for introspection", definition.sourceLocation))
      }
    }
  }

  /**
   * This is not in the specification per-se but in our use case, that will help catch some cases when users mistake
   * graphql operations for schemas
   */
  private fun GQLDocument.validateNotExecutable() {
    definitions.firstOrNull { it is GQLOperationDefinition || it is GQLFragmentDefinition }
        ?.let {
          issues.add(Issue.ValidationError("Found an executable definition. Schemas should not contain operations or fragments.", it.sourceLocation))
        }
  }
}
