package com.apollographql.apollo3.gradle.api

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

class ApolloDependencies(private val handler: DependencyHandler) {
  val api: Dependency get() = handler.create("com.apollographql.apollo3:apollo-api")
  val runtime: Dependency get() = handler.create("com.apollographql.apollo3:apollo-runtime")
  val normalizedCache: Dependency get() = handler.create("com.apollographql.apollo3:apollo-normalized-cache")
  val normalizedCacheSqlite: Dependency get() = handler.create("com.apollographql.apollo3:apollo-normalized-cache-sqlite")
  val mockServer: Dependency get() = handler.create("com.apollographql.apollo3:apollo-mockserver")
  val ast: Dependency get() = handler.create("com.apollographql.apollo3:apollo-ast")
}
