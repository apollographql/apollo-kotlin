package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.findTargetName
import com.apollographql.apollo3.compiler.internal.SchemaSerializer
import kotlinx.serialization.Serializable

/**
 * A [Schema] linked with other options that are bound to this schema and need to be the same in all modules
 * using this schema
 */
@Serializable
class CodegenSchema(
    @Serializable(with = SchemaSerializer::class)
    val schema: Schema,
    val normalizedPath: String,
    val scalarMapping: Map<String, ScalarInfo>,
    val generateDataBuilders: Boolean
)

internal class CodegenType(
    val name: String,
    val targetName: String?,
)

internal fun CodegenSchema.allTypes(): List<CodegenType> {
  return schema.typeDefinitions.values.map {
    CodegenType(it.name, it.directives.findTargetName(schema))
  }.sortedBy { it.name }
}
