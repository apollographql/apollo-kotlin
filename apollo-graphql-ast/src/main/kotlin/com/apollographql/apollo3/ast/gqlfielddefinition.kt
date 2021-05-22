package com.apollographql.apollo3.ast

internal fun GQLFieldDefinition.isDeprecated(): Boolean {
  return directives.firstOrNull { it.name == "deprecated" } != null
}