package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun FileSpec.Builder.addInternal(patterns: List<String>): FileSpec.Builder = apply {
  val nameRegexes = patterns.map { Regex(it) }

  members.replaceAll { member ->

    val memberName = when (member) {
      is TypeSpec -> member.name!!
      is FunSpec -> member.name
      is PropertySpec -> member.name
      else -> error("Unsupported member: $member")
    }
    val match = nameRegexes.any {
      it.matches("$packageName.$memberName") ||
          // Also match response adapters and selections, so callers can pass operation names directly
          it.matches(packageName + "." + (memberName.removeSuffix("_ResponseAdapter"))) ||
          it.matches(packageName + "." + (memberName.removeSuffix("Selections")))
    }
    if (!match) {
      return@replaceAll member
    }
    when (member) {
      is TypeSpec -> {
        if (member.modifiers.contains(KModifier.PRIVATE)) {
          member
        } else {
          member.toBuilder()
              .addModifiers(KModifier.INTERNAL)
              .build()
        }
      }

      is FunSpec -> {
        if (member.modifiers.contains(KModifier.PRIVATE)) {
          member
        } else {
          member.toBuilder()
              .addModifiers(KModifier.INTERNAL)
              .build()
        }
      }

      is PropertySpec -> {
        if (member.modifiers.contains(KModifier.PRIVATE)) {
          member
        } else {
          member.toBuilder()
              .addModifiers(KModifier.INTERNAL)
              .build()
        }
      }

      else -> error("Top Level $member is not supported")
    }
  }
}
