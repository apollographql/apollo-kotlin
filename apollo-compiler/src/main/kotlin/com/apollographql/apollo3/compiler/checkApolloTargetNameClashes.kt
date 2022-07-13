@file:JvmName("-checkApolloDuplicateTargetNames")

package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.findTargetName
import com.apollographql.apollo3.compiler.codegen.CodegenLayout

/**
 * Checks that targetNames don't clash with other class name.
 * Note: case is ignored in comparison because we assume the file system is case-insensitive.
 */
internal fun checkApolloTargetNameClashes(schema: Schema): List<Issue> {
  val issues = mutableListOf<Issue>()

  val typesWithTargetName: Map<GQLTypeDefinition, String?> = schema
      .typeDefinitions
      .values
      // Sort to ensure consistent results
      .sortedBy { it.name }
      .associateWith { it.directives.findTargetName(schema) }
  val usedNames = mutableMapOf<String, GQLTypeDefinition>()

  // 1. Collect unique names for types without a targetName
  for ((type, _) in typesWithTargetName.filterValues { it == null }) {
    val name = CodegenLayout.uniqueName(type.name, usedNames.keys.toSet())
    usedNames[name.lowercase()] = type
  }

  // 2. Check targetName for types that define it
  for ((type, targetName) in typesWithTargetName.filterValues { it != null }) {
    val name = targetName!!.lowercase()
    if (usedNames.containsKey(name)) {
      val typeForUsedName = usedNames[name]!!
      issues.add(
          Issue.ReservedEnumValueName(
              message = "'${targetName}' cannot be used as a target name for '${type.name}' because it clashes with '${typeForUsedName.name}' defined at ${typeForUsedName.sourceLocation.pretty()}",
              sourceLocation = type.sourceLocation
          )
      )
    } else {
      usedNames[name] = type
    }
  }
  return issues
}

private val GQLTypeDefinition.directives: List<GQLDirective>
  get() = when (this) {
    is GQLEnumTypeDefinition -> directives
    is GQLInputObjectTypeDefinition -> directives
    is GQLInterfaceTypeDefinition -> directives
    is GQLObjectTypeDefinition -> directives
    is GQLScalarTypeDefinition -> directives
    is GQLUnionTypeDefinition -> directives
  }
