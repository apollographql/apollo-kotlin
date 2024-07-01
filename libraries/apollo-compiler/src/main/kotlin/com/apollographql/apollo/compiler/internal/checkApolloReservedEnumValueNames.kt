@file:JvmName("-checkApolloReservedEnumValueNames")

package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.ReservedEnumValueName
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.findTargetName

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
      if (name in enumNames) {
        issues.add(
            ReservedEnumValueName(
                message = "'${targetName}' is already defined in this enum, please use a different name",
                sourceLocation = value.sourceLocation
            )
        )
      } else {
        // all good
        enumNames.add(name)
      }
    }
  }
  return issues
}
