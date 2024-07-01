package com.apollographql.apollo.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
actual annotation class ApolloAdaptableWith(public actual val adapter: KClass<*>)
