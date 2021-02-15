package com.apollographql.apollo.compiler.introspection

internal class UnkownTypeException(message: String) : RuntimeException(message) {

  override fun fillInStackTrace(): Throwable = this
}
