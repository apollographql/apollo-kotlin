package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.TargetLanguage

internal fun generateFilterNotNull(targetLanguage: TargetLanguage, isKmp: Boolean): Boolean? {
  return if (targetLanguage == TargetLanguage.JAVA) {
    null
  } else {
    isKmp
  }
}

