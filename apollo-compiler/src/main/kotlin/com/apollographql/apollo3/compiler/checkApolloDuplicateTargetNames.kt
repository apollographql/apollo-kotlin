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

internal fun checkApolloDuplicateTargetNames(schema: Schema): List<Issue> {
  val issues = mutableListOf<Issue>()

  val visitedTypes = mutableListOf<GQLTypeDefinition>()
  for (type in schema
      .typeDefinitions
      .values
  ) {
    val targetName = type.directives.findTargetName(schema)
    if (targetName == null) {
      visitedTypes.add(type)
      continue
    }

    val existingTypeWithSameName = visitedTypes.find { visitedType ->
      val visitedTypeTargetName = visitedType.directives.findTargetName(schema)

      // Ignore case because we assume the file system is case-insensitive.
      (visitedTypeTargetName ?: visitedType.name).equals(targetName, ignoreCase = true)
    }
    if (existingTypeWithSameName != null) {
      issues.add(
          Issue.ReservedEnumValueName(
              message = "A type named '${targetName}' is already defined, please use a different target name. First definition is: ${existingTypeWithSameName.sourceLocation.pretty()}",
              sourceLocation = type.sourceLocation
          )
      )
    }
    visitedTypes.add(type)
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
