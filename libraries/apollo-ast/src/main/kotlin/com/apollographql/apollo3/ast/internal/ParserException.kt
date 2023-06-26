package com.apollographql.apollo3.ast.internal

internal class ParserException(override val message: String, val token: Token) : Exception(message)
