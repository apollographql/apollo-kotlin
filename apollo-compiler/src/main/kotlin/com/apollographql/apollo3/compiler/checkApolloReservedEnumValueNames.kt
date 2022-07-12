@file:JvmName("-checkApolloReservedEnumValueNames")

package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.findTargetName

internal fun checkApolloReservedEnumValueNames(schema: Schema): List<Issue> {
  val issues = mutableListOf<Issue>()

  for (enumDefinition in schema
      .typeDefinitions
      .values
      .filterIsInstance<GQLEnumTypeDefinition>()
  ) {
    val enumNames = mutableSetOf<String>()
    for (value in enumDefinition.enumValues) {
      val targetName = value.directives.findTargetName(schema)

      val name = targetName ?: value.name
      when {
        name.isApolloReservedEnumValueName() -> {
          val message = if (targetName == null) {
            "'$name' is a reserved enum value name, please use the @targetName directive to specify a target name"
          } else {
            "'$name' is a reserved enum value name, please use a different name"
          }
          issues.add(
              Issue.ReservedEnumValueName(
                  message = message,
                  sourceLocation = value.sourceLocation
              )
          )
        }
        name in enumNames -> {
          issues.add(
              Issue.ReservedEnumValueName(
                  message = "'${targetName}' is already defined in this enum, please use a different name",
                  sourceLocation = value.sourceLocation
              )
          )
        }
        else -> {
          // all good
          enumNames.add(name)
        }
      }
    }
  }
  return issues
}
