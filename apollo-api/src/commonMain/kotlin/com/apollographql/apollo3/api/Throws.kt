package com.apollographql.apollo3.api

import kotlin.reflect.KClass

expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)