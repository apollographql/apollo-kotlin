package com.apollographql.apollo3.ast

internal fun GQLEnumValueDefinition.isDeprecated(): Boolean {
  return directives.firstOrNull { it.name == "deprecated" } != null
}

