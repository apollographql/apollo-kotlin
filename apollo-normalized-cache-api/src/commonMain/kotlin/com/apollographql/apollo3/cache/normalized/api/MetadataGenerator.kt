package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable

@ApolloExperimental
interface MetadataGenerator {
  fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?>
}

@ApolloExperimental
class MetadataGeneratorContext(
    val field: CompiledField,
    val variables: Executable.Variables,
)

@ApolloExperimental
object EmptyMetadataGenerator : MetadataGenerator {
  override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> = emptyMap()
}
