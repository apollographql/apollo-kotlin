package com.apollographql.apollo.ast

internal fun GQLEnumValueDefinition.isDeprecated(): Boolean {
  return directives.firstOrNull { it.name == "deprecated" } != null
}

