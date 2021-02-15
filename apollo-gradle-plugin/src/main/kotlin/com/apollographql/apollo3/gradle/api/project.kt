package com.apollographql.apollo.gradle.api

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

val Project.kotlinMultiplatformExtension
  get() = extensions.findByName("kotlin") as? KotlinMultiplatformExtension

val Project.kotlinJvmExtension
  get() = extensions.findByName("kotlin") as? KotlinProjectExtension

val Project.androidExtension
  get() = extensions.findByName("android") as? BaseExtension

val Project.kotlinMultiplatformExtensionOrFail
  get() = kotlinMultiplatformExtension ?: throw IllegalStateException("ApolloGraphQL: no 'kotlin' extension found. Did you apply the Kotlin multiplatform plugin?")

val Project.kotlinJvmExtensionOrFail
  get() = kotlinJvmExtension ?: throw IllegalStateException("ApolloGraphQL: no 'kotlin' extension found. Did you apply the Kotlin jvm plugin?")

val Project.androidExtensionOrFail
  get() = androidExtension ?: throw IllegalStateException("ApolloGraphQL: no 'android' extension found. Did you apply the Android plugin?")

val Project.isKotlinMultiplatform get() = pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")
