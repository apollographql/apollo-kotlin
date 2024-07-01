package com.apollographql.apollo.ast.internal

internal class ParserException(override val message: String, val token: Token) : Exception(message)
