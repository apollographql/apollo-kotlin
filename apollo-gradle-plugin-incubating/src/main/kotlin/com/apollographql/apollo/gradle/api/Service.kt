package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.gradle.internal.DefaultIntrospection
import groovy.lang.Closure

interface Service: CompilerParams {
  fun introspection(closure: Closure<DefaultIntrospection>)
}