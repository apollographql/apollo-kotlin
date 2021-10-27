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

    private val REGEX = Regex("(\\d)\\.(\\d)(\\.(\\d).*)?")

    fun parse(s: String): VersionNumber {
      val matchResult = REGEX.find(s) ?: error("Could not parse '$s' as a version number")
      val (major, minor, patch) = matchResult.destructured
      return VersionNumber(
          major.toInt(),
          minor.toInt(),
          // Allow e.g. "1.4" to be interpreted as "1.4.0"
          patch.ifEmpty { "0" }.toInt(),
      )
    }
  }
}
