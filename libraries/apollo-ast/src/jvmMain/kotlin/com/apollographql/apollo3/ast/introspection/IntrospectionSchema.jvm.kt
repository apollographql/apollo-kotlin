@file:JvmMultifileClass
@file:JvmName("IntrospectionSchemaKt")
package com.apollographql.apollo3.ast.introspection

import okio.buffer
import okio.sink
import okio.source
import java.io.File

fun File.toIntrospectionSchema() = inputStream().source().buffer().toIntrospectionSchema("from `$this`")
fun IntrospectionSchema.writeTo(file: File) {
  file.outputStream().sink().buffer().use {
    it.writeUtf8(json.encodeToString(IntrospectionSchema.serializer(), this))
  }
}
