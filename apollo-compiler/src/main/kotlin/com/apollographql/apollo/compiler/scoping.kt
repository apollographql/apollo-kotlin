package com.apollographql.apollo.compiler

internal inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T = if (condition) apply(block) else this