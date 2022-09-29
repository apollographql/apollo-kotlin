package com.apollographql.apollo3.gradle.internal

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention

// Copied from kotlin plugin
private fun Any.getConvention(name: String): Any? =
    (this as HasConvention).convention.plugins[name]

// Copied from kotlin plugin
internal fun AndroidSourceSet.kotlinSourceSet(): SourceDirectorySet? {
  val convention = (getConvention("kotlin") ?: getConvention("kotlin2js")) ?: return null
  val kotlinSourceSet = convention.javaClass.interfaces.find { it.name == "org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet" }
  val getKotlin = kotlinSourceSet?.methods?.find { it.name == "getKotlin" } ?: return null
  return getKotlin(convention) as? SourceDirectorySet
}

