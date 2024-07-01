package com.apollographql.apollo.annotations

import kotlin.reflect.KClass


expect annotation class ApolloAdaptableWith(public val adapter: KClass<*>)