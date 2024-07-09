package com.apollographql.apollo.debugserver.internal.initializer

import android.content.Context
import androidx.startup.Initializer

internal class ApolloDebugServerInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    packageName = context.packageName
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

  companion object {
    var packageName: String? = null
  }
}
