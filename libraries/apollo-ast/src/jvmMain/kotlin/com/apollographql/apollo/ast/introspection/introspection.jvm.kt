@file:JvmMultifileClass
@file:JvmName("Introspection")
package com.apollographql.apollo.ast.introspection

import okio.buffer
import okio.sink
import okio.source
import java.io.File

fun File.toIntrospectionSchema(): IntrospectionSchema = inputStream().source().buffer().toIntrospectionSchema(absolutePath)

fun ApolloIntrospectionSchema.writeTo(file: File) {
  file.outputStream().sink().buffer().use {
    it.writeUtf8(this.toJson())
  }
}
