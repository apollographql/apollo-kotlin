package com.apollographql.apollo.ast.internal

/**
 * The mapping between the name of a directive and its canonical name.
 *
 * Those names do not match in the case of:
 * - imports with `@link`
 * - ignoring directives with `@ignore`
 */
internal class DirectivesMapping(val names: Map<String, String>) {
  fun canonicalName(schemaName: String): String? {
    return names.get(schemaName)
  }
}