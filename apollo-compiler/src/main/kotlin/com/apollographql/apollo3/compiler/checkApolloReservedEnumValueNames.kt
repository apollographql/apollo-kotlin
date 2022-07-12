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
    val enumNames = mutableListOf<String>()
    for (value in enumDefinition.enumValues) {
      val targetName = value.directives.findTargetName(schema)

      if (value.name.isApolloReservedEnumValueName()) {
        if (targetName == null) {
          issues.add(
              Issue.ReservedEnumValueName(
                  message = "'${value.name}' is a reserved enum value name, please use the @targetName directive to specify a target name",
                  sourceLocation = value.sourceLocation
              )
          )
        }
      }

      if (targetName != null) {
        if (targetName.isApolloReservedEnumValueName()) {
          issues.add(
              Issue.ReservedEnumValueName(
                  message = "'${targetName}' is a reserved enum value name, please use a different name",
                  sourceLocation = value.sourceLocation
              )
          )
        } else if (targetName in enumNames) {
          issues.add(
              Issue.ReservedEnumValueName(
                  message = "'${targetName}' is already defined in this enum, please use a different name",
                  sourceLocation = value.sourceLocation
              )
          )
        } else {
          enumNames.add(targetName)
        }
      } else {
        enumNames.add(value.name)
      }
    }
  }
  return issues
}
