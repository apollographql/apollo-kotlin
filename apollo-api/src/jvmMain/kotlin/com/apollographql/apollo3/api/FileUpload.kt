@file:JvmName("FileUpload")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_3_3
import okio.buffer
import okio.source
import java.io.File

@Deprecated("Use File.toUpload() instead")
@ApolloDeprecatedSince(v3_3_3)
fun DefaultUpload.Builder.content(file: File): DefaultUpload.Builder {
  return content { sink ->
    file.source().buffer().use { sink.writeAll(it) }
  }.contentLength(file.length())
}

fun File.toUpload(contentType: String): DefaultUpload {
  return DefaultUpload.Builder()
      .content { sink ->
        source().buffer().use { sink.writeAll(it) }
      }
      .contentLength(length())
      .contentType(contentType)
      .fileName(name)
      .build()
}

@Deprecated(
    "This is a helper function to help migrating to 3.x and will be removed in a future version",
    ReplaceWith(
        "File(filePath).toUpload(mimetype)",
        imports = ["java.io.File"]
    )
)
@ApolloDeprecatedSince(v3_0_0)
fun create(mimetype: String, filePath: String): Upload {
  return File(filePath).toUpload(mimetype)
}
