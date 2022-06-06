@file:JvmName("-checkApolloReservedEnumValueNames")
package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.findTargetName

@ApolloInternal
fun checkApolloReservedEnumValueNames(schema: Schema): List<Issue> {
  val issues = mutableListOf<Issue>()

  for (enumDefinition in schema
      .typeDefinitions
      .values
      .filterIsInstance<GQLEnumTypeDefinition>()
  ) {
    for (value in enumDefinition.enumValues) {
      if (value.name.isApolloReservedEnumValueName()) {
        val targetName = value.directives.findTargetName(schema)
        if (targetName == null) {
          issues.add(
              Issue.ReservedEnumValueName(
                  message = "'${value.name}' is a reserved enum value name, please use the @targetName directive to specify a target name",
                  sourceLocation = value.sourceLocation
              )
          )
        } else if (targetName.isApolloReservedEnumValueName()) {
          issues.add(
              Issue.ReservedEnumValueName(
                  message = "'${value.name}' is a reserved enum value name, please use a different name",
                  sourceLocation = value.sourceLocation
              )
          )
        }
      }
    }
  }
  return issues
}
