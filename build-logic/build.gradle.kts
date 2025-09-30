import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin

plugins {
  alias(libs.plugins.kotlin.jvm.latest)
  alias(libs.plugins.kotlin.sam)
  alias(libs.plugins.compat.patrouille)
}

plugins.apply(SamWithReceiverGradleSubplugin::class.java)
extensions.configure(SamWithReceiverExtension::class.java) {
  annotations(HasImplicitReceiver::class.qualifiedName!!)
}

group = "com.apollographql.apollo"

dependencies {
  compileOnly(gradleApi())
  compileOnly(libs.dgp)

  // We add all the plugins to the classpath here so that they are loaded with proper conflict resolution
  // See https://github.com/gradle/gradle/issues/4741
  implementation(libs.okhttp)
  implementation(libs.dokka)

  /**
   * Ktor for the websocket tests in integration tests
   */
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.cors)
  implementation(libs.ktor.server.websockets)

  implementation(libs.kotlinx.benchmark)
  implementation(libs.licensee)
  implementation(libs.apollo.execution.gradle.plugin)
  implementation(libs.compat.patrouille)

  implementation(libs.android.plugin)
  implementation(libs.librarian)
  implementation(libs.nmcp)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.poet.java)
  implementation(libs.poet.kotlin)
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
  runtimeOnly(libs.gratatouille)
  runtimeOnly(libs.kotlinx.binarycompatibilityvalidator)
}

compatPatrouille {
  java(17)
  kotlin(embeddedKotlinVersion)
}
