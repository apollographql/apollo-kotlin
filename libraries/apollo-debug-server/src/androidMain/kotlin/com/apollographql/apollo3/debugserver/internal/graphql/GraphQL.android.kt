package com.apollographql.apollo3.debugserver.internal.graphql

import com.apollographql.apollo3.debugserver.internal.server.Server
import okio.buffer
import okio.source
import kotlin.reflect.KClass

internal actual fun getExecutableSchema(): String = Server::class.java.classLoader!!
    .getResourceAsStream("schema.graphqls")!!
    .source()
    .buffer()
    .readUtf8()
