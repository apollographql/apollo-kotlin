package com.apollographql.apollo3.gradle.internal

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_JS_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

// Copied from kotlin plugin
private fun Any.getConvention(name: String): Any? =
    (this as HasConvention).convention.plugins[name]

// Copied from kotlin plugin
internal fun AndroidSourceSet.kotlinSourceSet(): SourceDirectorySet? {
  val convention = (getConvention(KOTLIN_DSL_NAME) ?: getConvention(KOTLIN_JS_DSL_NAME)) ?: return null
  val kotlinSourceSetIface =
      convention.javaClass.interfaces.find { it.name == KotlinSourceSet::class.qualifiedName }
  val getKotlin = kotlinSourceSetIface?.methods?.find { it.name == "getKotlin" } ?: return null
  return getKotlin(convention) as? SourceDirectorySet
}

