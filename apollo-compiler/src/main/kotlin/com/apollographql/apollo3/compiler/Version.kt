package com.apollographql.apollo3.compiler

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
  fun isAtLeast(version: Version): Boolean {
    return major * 10_000 + minor * 100 + patch >= version.major * 10_000 + version.minor * 100 + version.patch
  }

  companion object {
    val KOTLIN_1_5 = Version(1, 5, 0)

    private val REGEX = Regex("(\\d)\\.(\\d)\\.(\\d).*")

    fun parse(s: String): Version {
      val matchResult = REGEX.find(s) ?: return Version(0, 0, 0)
      val (major, minor, patch) = matchResult.destructured
      return Version(
          major.toInt(),
          minor.toInt(),
          patch.toInt(),
      )
    }
  }
}
