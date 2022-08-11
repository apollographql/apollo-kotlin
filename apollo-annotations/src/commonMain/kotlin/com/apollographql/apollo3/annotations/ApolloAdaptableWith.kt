package com.apollographql.apollo3.annotations

import kotlin.reflect.KClass


expect annotation class ApolloAdaptableWith(public val adapter: KClass<*>)