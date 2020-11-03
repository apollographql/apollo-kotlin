package com.apollographql.apollo.compiler.parser.error

import com.apollographql.apollo.compiler.ir.SourceLocation
import org.antlr.v4.runtime.Token

internal class ParseException(message: String, val sourceLocation: SourceLocation) : RuntimeException(message) {
  constructor(message: String, token: Token) : this(
      message = message,
      sourceLocation = SourceLocation(
          line = token.line,
          position = token.charPositionInLine
      )
  )

  constructor(message: String) : this(
      message = message,
      sourceLocation = SourceLocation.UNKNOWN
  )

  override fun fillInStackTrace(): Throwable = this
}
