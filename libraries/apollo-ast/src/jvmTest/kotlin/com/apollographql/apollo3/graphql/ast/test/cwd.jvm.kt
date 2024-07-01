package com.apollographql.apollo.graphql.ast.test

import java.lang.System.getenv

internal actual val CWD: String
  get() = getenv("MODULE_ROOT") ?: error("\$MODULE_ROOT not found")