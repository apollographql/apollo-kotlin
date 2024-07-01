package com.apollographql.apollo.ast.internal

internal class LexerException(override val message: String, val pos: Int, val line: Int, val column: Int, cause: Throwable?) : Exception(message, cause)
