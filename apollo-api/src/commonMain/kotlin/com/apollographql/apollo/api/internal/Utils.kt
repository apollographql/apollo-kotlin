package com.apollographql.apollo.api.internal

import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

/**
 * Contains utility methods for checking Preconditions
 */
@Suppress("FunctionName")
object Utils {

  /**
   * Checks if the object is null. Returns the object if it is not null, else throws a NullPointerException with the
   * error message.
   *
   * @param reference the object whose nullability has to be checked
   * @param errorMessage the message to use with the NullPointerException
   * @param <T> the value type
   * @return The object itself
   * @throws NullPointerException if the object is null
   */
  @JvmStatic
  @JvmName("checkNotNull")
  fun <T> __checkNotNull(reference: T?, errorMessage: Any?): T {
    if (reference == null) {
      throw NullPointerException(errorMessage.toString())
    }
    return reference
  }

  /**
   * Checks if the object is null. Returns the object if it is not null, else throws a NullPointerException.
   *
   * @param reference the object whose nullability has to be checked
   * @param <T> the value type
   * @return The object itself
   * @throws NullPointerException if the object is null
   */
  @JvmStatic
  @JvmName("checkNotNull")
  fun <T> __checkNotNull(reference: T?): T {
    if (reference == null) {
      throw NullPointerException()
    }
    return reference
  }
}
