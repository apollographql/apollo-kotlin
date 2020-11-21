package com.apollographql.apollo.compiler.parser.introspection

import com.apollographql.apollo.compiler.ir.SourceLocation
import org.antlr.v4.runtime.Token

internal class UnkownTypeException(message: String, val sourceLocation: SourceLocation) : RuntimeException(message) {
  constructor(message: String) : this(
      message = message,
      sourceLocation = SourceLocation.UNKNOWN
  )

  override fun fillInStackTrace(): Throwable = this
}
