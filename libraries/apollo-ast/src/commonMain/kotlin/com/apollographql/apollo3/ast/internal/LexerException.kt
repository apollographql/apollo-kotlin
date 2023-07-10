package com.apollographql.apollo3.ast.internal

internal class LexerException(override val message: String, val line: Int, val column: Int, cause: Throwable?) : Exception(message, cause)
