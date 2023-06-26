package com.apollographql.apollo3.ast.internal

internal class ScannerException(override val message: String, val line: Int, val column: Int) : Exception(message)
