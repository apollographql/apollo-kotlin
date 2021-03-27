package com.apollographql.apollo.compiler.codegen.kotlin

import com.squareup.kotlinpoet.ClassName

/**
 * A list of okio [TypeName]
 * We cannot use things like BufferedSource::class.asTypeName() as that would get relocated by the shadow plugin and produce illegal code
 */
object OkioKotlinTypeName {
  val packageName = rot1("njhn")

  val BufferedSource = ClassName(packageName = packageName, "BufferedSource")
  val ByteString = ClassName(packageName = packageName, "ByteString")
  val Buffer = ClassName(packageName = packageName, "Buffer")
  val IOException = ClassName(packageName = packageName, "IOException")
}