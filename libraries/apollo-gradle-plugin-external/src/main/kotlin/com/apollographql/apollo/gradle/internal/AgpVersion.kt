package com.apollographql.apollo.gradle.internal

/**
 * This function throws if AGP is not in the current context classloader
 */
fun agpVersion(): String {
  val version = Class.forName("com.android.Version")

  val field = version.declaredFields.firstOrNull { it.name == "ANDROID_GRADLE_PLUGIN_VERSION" }
  check(field != null) {
    "Apollo: cannot find ANDROID_GRADLE_PLUGIN_VERSION"
  }

  val ret = field.get(null)
  check(ret is String) {
    "Apollo: ANDROID_GRADLE_PLUGIN_VERSION must be a String (found '$ret')"
  }
  return ret
}
