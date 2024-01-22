package com.apollographql.apollo3.compiler.hooks.internal

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Adds the `internal` modifier to types matching the given names.
 *
 * @param namePatterns a set of regex patterns to match type names against. If a type name (including its package) matches,
 * it will be made internal. Examples:
 * - to match all classes, use `".*"`
 * - to match classes under a specific package, use `"com\\.example\\.subpackage\\..*"`
 * - to match a specific operation, use `"com\\.example\\.MyQuery"`
 */
internal fun addInternal(fileSpecs: List<FileSpec>, namePatterns: Set<String>): List<FileSpec>{
  val nameRegexes = namePatterns.map { Regex(it) }

  return fileSpecs
      .map { fileSpec ->
        fileSpec.toBuilder()
            .apply {
              members.replaceAll { member ->

                val memberName = when (member) {
                  is TypeSpec -> member.name!!
                  is FunSpec -> member.name
                  is PropertySpec -> member.name
                  else -> error("Unsupported member: $member")
                }
                val match = nameRegexes.any {
                  it.matches(fileSpec.packageName + "." + memberName) ||
                      // Also match response adapters and selections, so callers can pass operation names directly
                      it.matches(fileSpec.packageName + "." + (memberName.removeSuffix("_ResponseAdapter"))) ||
                      it.matches(fileSpec.packageName + "." + (memberName.removeSuffix("Selections")))
                }
                if(!match) {
                  return@replaceAll member
                }
                when(member) {
                  is TypeSpec -> {
                    member.toBuilder()
                        .addModifiers(KModifier.INTERNAL)
                        .build()
                  }
                  is FunSpec -> {
                    member.toBuilder()
                        .addModifiers(KModifier.INTERNAL)
                        .build()
                  }
                  is PropertySpec -> {
                    member.toBuilder()
                        .addModifiers(KModifier.INTERNAL)
                        .build()
                  }
                  else -> error("Top Level $member is not supported")
                }
              }
            }
            .build()
      }
}