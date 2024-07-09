package com.apollographql.apollo.cache.normalized.sql

import android.content.Context
import androidx.startup.Initializer

class ApolloInitializer: Initializer<Unit> {
  override fun create(context: Context) {
    Companion.context = context
    return
  }

  override fun dependencies(): MutableList<Class<out Initializer<*>>> {
    return mutableListOf()
  }

  companion object {
    internal lateinit var context: Context
  }
}