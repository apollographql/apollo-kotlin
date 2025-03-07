@file:JvmMultifileClass
@file:JvmName("Introspection")

package com.apollographql.apollo.ast.introspection

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.ast.HOST_FILESYSTEM
import okio.Path
import okio.buffer
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

@ApolloExperimental
fun Path.toIntrospectionSchema(): IntrospectionSchema {
  return HOST_FILESYSTEM
      .source(this)
      .buffer()
      .toIntrospectionSchema(toString())
}
