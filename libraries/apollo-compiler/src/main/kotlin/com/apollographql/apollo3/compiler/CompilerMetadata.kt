package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okio.buffer
import okio.sink
import okio.source
import java.io.File

/**
 * Compilation unit specific metadata that is specific to a given invocation of the compiler
 */
@JsonClass(generateAdapter = true)
data class CompilerMetadata(
    /**
     * resolver info used by the codegen to lookup already existing ClassNames
     */
    val resolverInfo: ResolverInfo,
)

private val adapter =    Moshi.Builder()
    .build()
    .adapter(CompilerMetadata::class.java)

fun CompilerMetadata.writeTo(file: File) {
  file.sink().buffer().use {
    adapter.toJson(it, this)
  }
}

fun File.toCompilerMetadata(): CompilerMetadata {
  return source().buffer().use {
    adapter.fromJson(it)!!
  }
}