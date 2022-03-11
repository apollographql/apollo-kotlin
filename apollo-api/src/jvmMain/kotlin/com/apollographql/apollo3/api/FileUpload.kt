@file:JvmName("FileUpload")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import okio.buffer
import okio.source
import java.io.File

fun DefaultUpload.Builder.content(file: File): DefaultUpload.Builder {
  return content(file.source().buffer()).contentLength(file.length())
}

@Deprecated(
  "This is a helper function to help migrating to 3.x and will be removed in a future version",
  ReplaceWith(
    "DefaultUpload.Builder().content(filePath).contentType(mimetype).build()",
    imports = ["com.apollographql.apollo3.api.DefaultUpload"]
  )
)
@ApolloDeprecatedSince(v3_0_0)
fun create(mimetype: String, filePath: String): Upload {
  val file = File(filePath)
  return DefaultUpload.Builder()
      .content(file)
      .contentType(mimetype)
      .build()
}
