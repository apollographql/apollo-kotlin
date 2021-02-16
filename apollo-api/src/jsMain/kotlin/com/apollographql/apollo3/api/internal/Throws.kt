package com.apollographql.apollo3.api.internal

import kotlin.reflect.KClass

actual annotation class Throws actual constructor(actual vararg val exceptionClasses: KClass<out Throwable>)
