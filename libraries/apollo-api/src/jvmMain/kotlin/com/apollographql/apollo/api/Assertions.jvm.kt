@file:JvmMultifileClass
@file:JvmName("Assertions")

package com.apollographql.apollo.api

@SafeVarargs
// Optional and its methods generate 'NewApi' warnings since it's Android API level 24+. It can safely be suppressed as this method
// is only referenced in projects that use Optional already.
@Suppress("NewApi")
fun assertOneOf(vararg args: java.util.Optional<out java.util.Optional<*>>) {
  val presentArgs = args.filter { it.isPresent }
  if (presentArgs.size != 1) {
    throw IllegalArgumentException("@oneOf input must have one field set (got ${presentArgs.size})")
  }
  if (!presentArgs.first().get().isPresent) {
    throw IllegalArgumentException("The value set on @oneOf input field must be non-null")
  }
}
