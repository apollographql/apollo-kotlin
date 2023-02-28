package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okio.buffer
import okio.sink
import okio.source
import java.io.File

@JsonClass(generateAdapter = true)
data class CodegenMetadata internal constructor(
    /**
     * resolver info used by the codegen to lookup already existing ClassNames
     */
    internal val resolverInfo: ResolverInfo,
)

fun CodegenMetadata.schemaTypes(): Set<String> {
  return resolverInfo.entries.filter { it.key.kind == ResolverKeyKind.SchemaType }.map { it.key.id }.toSet()
}

private val adapter = Moshi.Builder()
    .build()
    .adapter(CodegenMetadata::class.java)

fun CodegenMetadata.writeTo(file: File) {
  file.sink().buffer().use {
    adapter.toJson(it, this)
  }
}

fun File.toCodegenMetadata(): CodegenMetadata {
  return source().buffer().use {
    adapter.fromJson(it)!!
  }
}