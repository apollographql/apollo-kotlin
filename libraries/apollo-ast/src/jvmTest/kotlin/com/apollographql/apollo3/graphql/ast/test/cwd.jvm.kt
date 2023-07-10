package com.apollographql.apollo3.graphql.ast.test

import java.lang.System.getenv

internal actual val CWD: String
  get() = getenv("MODULE_ROOT") ?: error("\$MODULE_ROOT not found")