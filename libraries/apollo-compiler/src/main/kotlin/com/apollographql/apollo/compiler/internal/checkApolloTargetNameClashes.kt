@file:JvmName("-checkApolloDuplicateTargetNames")

package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.ReservedEnumValueName
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.findTargetName
import com.apollographql.apollo.ast.pretty
import com.apollographql.apollo.compiler.uniqueName

/**
 * Checks that targetNames don't clash with other class name.
 * Note: case is ignored in comparison because we assume the file system is case-insensitive.
 */
internal fun checkApolloTargetNameClashes(schema: Schema): List<Issue> {
  val issues = mutableListOf<Issue>()

  /*
   * A list and not a Map: hashing a GQLTypeDefinition hashes its whole subtree, which is expensive for large schemas.
   */
  val typesWithTargetName: List<Pair<GQLTypeDefinition, String?>> = schema
      .typeDefinitions
      .values
      // Sort to ensure consistent results
      .sortedBy { it.name }
      .map { it to it.directives.findTargetName(schema) }
  val usedNames = HashMap<String, GQLTypeDefinition>(2 * typesWithTargetName.size)

  // 1. Collect unique names for types without a targetName
  for ((type, targetName) in typesWithTargetName) {
    if (targetName != null) continue
    // `usedNames.keys` is only read by `uniqueName`, no need to copy it
    val name = uniqueName(type.name, usedNames.keys)
    usedNames[name.lowercase()] = type
  }

  // 2. Check targetName for types that define it
  for ((type, targetName) in typesWithTargetName) {
    if (targetName == null) continue
    val name = targetName.lowercase()
    if (usedNames.containsKey(name)) {
      val typeForUsedName = usedNames[name]!!
      issues.add(
          ReservedEnumValueName(
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
