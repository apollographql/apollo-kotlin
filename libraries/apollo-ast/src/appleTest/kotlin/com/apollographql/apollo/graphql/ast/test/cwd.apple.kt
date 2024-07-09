package com.apollographql.apollo.graphql.ast.test

import kotlinx.cinterop.toKString
import platform.posix.getenv

internal actual val CWD: String
  get() = getenv("MODULE_ROOT")?.toKString() ?: error("\$MODULE_ROOT not found")