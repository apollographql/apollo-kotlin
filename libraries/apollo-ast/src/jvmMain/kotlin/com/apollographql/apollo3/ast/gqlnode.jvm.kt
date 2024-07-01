@file:JvmMultifileClass
@file:JvmName("GqlnodeKt")
package com.apollographql.apollo.ast

import okio.buffer
import okio.sink
import java.io.File

fun GQLNode.toUtf8(file: File, indent: String = "  ") = file.outputStream().sink().buffer().use {
  toUtf8(it, indent)
}
