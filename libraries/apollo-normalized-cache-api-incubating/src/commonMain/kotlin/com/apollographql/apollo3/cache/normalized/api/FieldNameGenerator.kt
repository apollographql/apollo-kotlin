package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable

@ApolloExperimental
interface FieldNameGenerator {
  fun getFieldName(context: FieldNameContext): String
}

@ApolloExperimental
class FieldNameContext(
    val parentType: String,
    val field: CompiledField,
    val variables: Executable.Variables,
)

@ApolloExperimental
object DefaultFieldNameGenerator : FieldNameGenerator {
  override fun getFieldName(context: FieldNameContext): String {
    return context.field.nameWithArguments(context.variables)
  }
}
