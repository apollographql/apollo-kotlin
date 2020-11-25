package com.apollographql.apollo.compiler.introspection

import com.apollographql.apollo.compiler.frontend.ir.SourceLocation

internal class UnkownTypeException(message: String, val sourceLocation: SourceLocation) : RuntimeException(message) {
  constructor(message: String) : this(
      message = message,
      sourceLocation = SourceLocation.UNKNOWN
  )

  override fun fillInStackTrace(): Throwable = this
}
