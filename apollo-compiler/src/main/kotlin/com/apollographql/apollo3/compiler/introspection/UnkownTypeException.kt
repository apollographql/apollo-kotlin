package com.apollographql.apollo3.compiler.introspection

internal class UnkownTypeException(message: String) : RuntimeException(message) {

  override fun fillInStackTrace(): Throwable = this
}
