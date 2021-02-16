package com.apollographql.apollo.api.internal

import kotlin.reflect.KClass

expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)
