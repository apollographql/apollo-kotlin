@file:JvmName("-Introspection")
package com.apollographql.apollo3.ast.introspection

import okio.buffer
import okio.sink
import okio.source
import java.io.File

fun File.toIntrospectionSchema() = inputStream().source().buffer().toIntrospectionSchema(absolutePath)

fun ApolloIntrospectionSchema.writeTo(file: File) {
  file.outputStream().sink().buffer().use {
    it.writeUtf8(this.toJson())
  }
}
