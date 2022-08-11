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

group = "com.apollographql.apollo3"

dependencies {
  compileOnly(libs.gradle.api)

  implementation(libs.okhttp)

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

// This shuts down a warning in Kotlin 1.5.30:
// 'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

gradlePlugin {
  plugins {
    register("apollo.library") {
      id = "apollo.library"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.LibraryConventionPlugin"
    }

    register("apollo.test.jvm") {
      id = "apollo.test.jvm"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.JvmTestConventionPlugin"
    }

    register("apollo.test.multiplatform") {
      id = "apollo.test.multiplatform"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.MultiplatformTestConventionPlugin"
    }

    register("apollo.test.android") {
      id = "apollo.test.android"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.AndroidTestConventionPlugin"
    }

    register("apollo.test.vanilla") {
      id = "apollo.test.vanilla"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.VanillaTestConventionPlugin"
    }
  }
}
