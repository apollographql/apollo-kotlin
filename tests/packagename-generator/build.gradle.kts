@file:Suppress("DEPRECATION")

import com.apollographql.apollo.compiler.PackageNameGenerator

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
}

apollo {
  service("service") {
    packageNameGenerator.set(object : PackageNameGenerator {
      override val version: String
        get() = "PackageNameGenerator.0"

      override fun packageName(filePath: String): String {
        return when {
          filePath.endsWith(".graphqls") -> "foo_schema"
          else -> "foo_operation"
        }
      }
    })
  }
}