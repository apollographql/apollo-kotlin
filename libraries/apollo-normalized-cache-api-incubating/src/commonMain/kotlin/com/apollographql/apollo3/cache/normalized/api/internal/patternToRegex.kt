package com.apollographql.apollo3.cache.normalized.api.internal


/**
 * A tentative to approximate the Sqlite LIKE operator with Regexes
 */
internal fun patternToRegex(pattern: String): Regex {
  val regex = buildString {
    var pendingEscape = false
    for (i in pattern.indices) {
      val cur = pattern[i]
      when {
        pendingEscape -> {
          when {
            cur == '\\' -> append("\\\\") // an escaped backslash is also an escape backslash in a regex
            cur == '%' -> append("%")
            cur == '_' -> append("_")
            else -> error("Invalid escape in pattern: $pattern")
          }
        }

        cur == '\\' -> pendingEscape = true
        cur == '%' -> append(".*")
        cur == '_' -> append(".")
        else -> {
          if (specialChars.contains(cur)) {
            // this needs to be escaped in the regex
            append("\\")
          }
          append(cur)
        }
      }
    }
  }

  return Regex(regex, option = RegexOption.IGNORE_CASE)
}

private val specialChars = "()^$.*?+{}"
