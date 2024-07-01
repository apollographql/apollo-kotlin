package com.apollographql.apollo.graphql.ast.test

// https://youtrack.jetbrains.com/issue/KT-49125
internal actual val CWD: String
  get() = js("globalThis.process.env['MODULE_ROOT']") as String? ?: error("\$MODULE_ROOT not found")