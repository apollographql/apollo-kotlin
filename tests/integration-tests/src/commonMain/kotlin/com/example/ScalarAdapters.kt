package com.example

import com.apollographql.apollo3.adapter.KotlinxInstantAdapter
import kotlin.jvm.JvmField

object ScalarAdapters {
  @JvmField
  val InstantAdapter = KotlinxInstantAdapter
}
