package com.apollographql.apollo3.compiler

data class VersionNumber(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
  fun isAtLeast(versionNumber: VersionNumber): Boolean {
    return major * 10_000 + minor * 100 + patch >= versionNumber.major * 10_000 + versionNumber.minor * 100 + versionNumber.patch
  }

  companion object {
    val KOTLIN_1_5 = VersionNumber(1, 5, 0)

    private val REGEX = Regex("(\\d)\\.(\\d)\\.(\\d).*")

    fun parse(s: String): VersionNumber {
      val matchResult = REGEX.find(s) ?: return VersionNumber(0, 0, 0)
      val (major, minor, patch) = matchResult.destructured
      return VersionNumber(
          major.toInt(),
          minor.toInt(),
          patch.toInt(),
      )
    }
  }
}
