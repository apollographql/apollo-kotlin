package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okio.buffer
import okio.sink
import okio.source
import java.io.File

@JsonClass(generateAdapter = true)
data class CodegenMetadata(
    /**
     * resolver info used by the codegen to lookup already existing ClassNames
     */
    val resolverInfo: ResolverInfo,
)

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