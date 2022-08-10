import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin

plugins {
  `embedded-kotlin`
  id("java-gradle-plugin")
}

plugins.apply(SamWithReceiverGradleSubplugin::class.java)
extensions.configure(SamWithReceiverExtension::class.java) {
  annotations(HasImplicitReceiver::class.qualifiedName!!)
}

dependencies {
  compileOnly(libs.gradle.api)

  implementation(libs.dokka.plugin)
  implementation(libs.dokka.base)

  // We add all the plugins to the classpath here so that they are loaded with proper conflict resolution
  // See https://github.com/gradle/gradle/issues/4741
  implementation(libs.android.plugin)
  implementation(libs.gradle.japicmp.plugin)
  implementation(libs.gradle.metalava.plugin)
  implementation(libs.vespene)

  // We want the KSP plugin to use the version from the classpath and not force a newer version
  // of the Gradle plugin
  if (System.getProperty("idea.sync.active") == null) {
    implementation(libs.kotlin.plugin)
    runtimeOnly(libs.ksp)
  } else {
    implementation(libs.kotlin.plugin.duringideasync)
    runtimeOnly(libs.ksp.duringideasync)
  }

  runtimeOnly(libs.sqldelight.plugin)
  runtimeOnly(libs.gradle.publish.plugin)
  runtimeOnly(libs.benmanes.versions)
  runtimeOnly(libs.gr8)
  runtimeOnly(libs.kotlinx.binarycompatibilityvalidator)
  // XXX: This is only needed for tests. We could have different build logic for different
  // builds but this seems just overkill for now
  runtimeOnly(libs.kotlin.allopen)
}

gradlePlugin {
  plugins {
    register("apollo.library.multiplatform") {
      id = "apollo.library.multiplatform"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.MultiplatformLibraryConventionPlugin"
    }

    register("apollo.library.jvm") {
      id = "apollo.library.jvm"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.JvmLibraryConventionPlugin"
    }

    register("apollo.library.android") {
      id = "apollo.library.android"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.AndroidLibraryConventionPlugin"
    }
  }
}
