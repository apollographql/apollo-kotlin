package com.apollographql.apollo3.compiler.internal

internal inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T = if (condition) apply(block) else this