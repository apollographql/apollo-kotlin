package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import java.io.InputStream

class ApolloMetadata(
    val schema: IntrospectionSchema,
    val options: MetadataOptions,
    val fragments: List<Fragment>
)

@JsonClass(generateAdapter = true)
class MetadataOptions(
    val schemaPackageName: String
)

fun MetadataOptions(inputStream: InputStream): MetadataOptions {
  val options =  Moshi.Builder().build().adapter(MetadataOptions::class.java).fromJson(inputStream.source().buffer())
  check(options != null) {
    "Apollo: Cannot parse metadata options"
  }
  return options
}