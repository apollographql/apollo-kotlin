package com.apollographql.apollo3

import android.annotation.SuppressLint
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
    @SuppressLint("StaticFieldLeak")
    internal var context: Context? = null
  }
}