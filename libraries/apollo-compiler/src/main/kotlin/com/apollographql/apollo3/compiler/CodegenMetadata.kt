package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File

@Serializable
data class CodegenMetadata internal constructor(
    /**
     * resolver info used by the codegen to lookup already existing ClassNames
     */
    internal val resolverInfo: ResolverInfo,
)

fun CodegenMetadata.schemaTypes(): Set<String> {
  return resolverInfo.entries.filter { it.key.kind == ResolverKeyKind.SchemaType }.map { it.key.id }.toSet()
}

@OptIn(ExperimentalSerializationApi::class)
fun CodegenMetadata.writeTo(file: File) {
  file.sink().buffer().use {
    Json.encodeToBufferedSink(CodegenMetadata.serializer(), this, it)
  }

}

@OptIn(ExperimentalSerializationApi::class)
fun File.toCodegenMetadata(): CodegenMetadata {
  return source().buffer().use {
    Json.decodeFromBufferedSource(CodegenMetadata.serializer(), it)
  }
}
