@file:JvmMultifileClass
@file:JvmName("GqlnodeKt")
package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloExperimental
import okio.Buffer
import okio.BufferedSink
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

@ApolloExperimental
fun GQLNode.toUtf8(sink: BufferedSink, indent: String = "  ") {
  val writer = SDLWriter(sink, indent)
  writer.write(this)
}

fun GQLNode.toUtf8(indent: String = "  "): String {
  val buffer = Buffer()
  toUtf8(buffer, indent)
  return buffer.readUtf8()
}
