import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.sam)
  alias(libs.plugins.compat.patrouille)
  id("java-gradle-plugin")
}

plugins.apply(SamWithReceiverGradleSubplugin::class.java)
extensions.configure(SamWithReceiverExtension::class.java) {
  annotations(HasImplicitReceiver::class.qualifiedName!!)
}

group = "com.apollographql.apollo.build"

dependencies {
  compileOnly(gradleApi())
  compileOnly(libs.dgp)

  implementation(libs.okhttp)

  implementation(libs.kotlinx.benchmark)
  implementation(libs.dokka)
  implementation(libs.licensee)
  implementation(libs.apollo.execution.gradle.plugin)
  implementation(libs.compat.patrouille)

  // We add all the plugins to the classpath here so that they are loaded with proper conflict resolution
  // See https://github.com/gradle/gradle/issues/4741
  implementation(libs.android.plugin)
  implementation(libs.gradle.japicmp.plugin)
  implementation(libs.vespene)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.poet.java)
  implementation(libs.poet.kotlin)
  implementation(libs.intellij.platform.plugin)
  implementation(libs.intellij.changelog)
  implementation(libs.androidx.lint.gradle.plugin)
  implementation(libs.kotlin.plugin.compose)

  implementation(libs.kotlin.plugin)
  implementation(libs.kotlin.plugin.annotations)

  runtimeOnly(libs.ksp)
  // XXX: This is only needed for tests. We could have different build logic for different
  // builds but this seems just overkill for now
  runtimeOnly(libs.kotlin.allopen)
  runtimeOnly(libs.kotlinx.serialization.plugin)
  runtimeOnly(libs.atomicfu.plugin)

  runtimeOnly(libs.sqldelight.plugin)
  runtimeOnly(libs.gradle.publish.plugin)
  runtimeOnly(libs.benmanes.versions)
  runtimeOnly(libs.gr8)
  runtimeOnly(libs.kotlinx.binarycompatibilityvalidator)
}


compatPatrouille {
  java(17)
  kotlin(embeddedKotlinVersion)
}

gradlePlugin {
  plugins {
    register("build.logic") {
      id = "build.logic"
      // This plugin is only used for loading the jar using the Marker but never applied
      // We don't need it.
      implementationClass = "build.logic.Unused"
    }
  }
}
