package com.apollographql.apollo.compiler.codegen.kotlin

import com.squareup.javapoet.ClassName


/**
 * A list of okio [TypeName]
 * We cannot use things like BufferedSource::class.asTypeName() as that would get relocated by the shadow plugin and produce illegal code
 */
object OkioJavaTypeName {
  val packageName = rot1("njhn")
  val BufferedSource = ClassName.get(packageName, "BufferedSource")
  val ByteString = ClassName.get(packageName, "ByteString")
  val Buffer = ClassName.get(packageName, "Buffer")
}

/**
 * Takes a string and returns the string with all chars rotated by one
 * This is used to hide constants that would otherwise be relocated by the shadow plugin
 */
fun rot1(str: String): String {
  val builder = StringBuilder(str.length)

  str.forEach {
    builder.append(it + 1)
  }
  return builder.toString()
}