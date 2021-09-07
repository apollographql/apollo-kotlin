@file:JvmName("Assertions")
package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.ApolloException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmName

fun checkFieldNotMissing(value: Any?, name: String) {
  if (value == null) {
    throw ApolloException("Field $name is missing")
  }
}
