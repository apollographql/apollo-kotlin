import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension

plugins {
  id("org.jetbrains.kotlin.jvm").version("1.9.21")
  id("org.jetbrains.kotlin.plugin.sam.with.receiver").version("1.9.21")
  id("java-gradle-plugin")
  id("net.mbonnin.golatac").version("0.0.3")
}

extensions.configure(SamWithReceiverExtension::class.java) {
  annotations(HasImplicitReceiver::class.qualifiedName!!)
}

group = "com.apollographql.apollo3.build"

golatac.init(file("../gradle/libraries.toml"))

dependencies {
  compileOnly(gradleApi())

  implementation(golatac.lib("okhttp"))

  implementation(golatac.lib("dokka.plugin"))
  implementation(golatac.lib("dokka.base"))

  // We add all the plugins to the classpath here so that they are loaded with proper conflict resolution
  // See https://github.com/gradle/gradle/issues/4741
  implementation(golatac.lib("android.plugin"))
  implementation(golatac.lib("gradle.japicmp.plugin"))
  implementation(golatac.lib("vespene"))
  implementation(golatac.lib("poet.java"))
  implementation(golatac.lib("poet.kotlin"))

  // We want the KSP plugin to use the version from the classpath and not force a newer version
  // of the Gradle plugin
  implementation(golatac.lib("kotlin.plugin"))
  runtimeOnly(golatac.lib("ksp"))
  // XXX: This is only needed for tests. We could have different build logic for different
  // builds but this seems just overkill for now
  runtimeOnly(golatac.lib("kotlin.allopen"))
  runtimeOnly(golatac.lib("kotlinx.serialization.plugin"))

  runtimeOnly(golatac.lib("sqldelight.plugin"))
  runtimeOnly(golatac.lib("gradle.publish.plugin"))
  runtimeOnly(golatac.lib("benmanes.versions"))
  runtimeOnly(golatac.lib("gr8"))
  runtimeOnly(golatac.lib("kotlinx.binarycompatibilityvalidator"))
}

java {
  // Keep in sync with CompilerOptions.kt
  toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

gradlePlugin {
  plugins {
    register("apollo.library") {
      id = "apollo.library"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.LibraryConventionPlugin"
    }

    register("apollo.test") {
      id = "apollo.test"
      implementationClass = "com.apollographql.apollo3.buildlogic.plugin.TestConventionPlugin"
    }
  }
}
