import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin

plugins {
  `embedded-kotlin`
  id("java-gradle-plugin")
  id("net.mbonnin.golatac") version "0.0.3"
}

plugins.apply(SamWithReceiverGradleSubplugin::class.java)
extensions.configure(SamWithReceiverExtension::class.java) {
  annotations(HasImplicitReceiver::class.qualifiedName!!)
}

group = "com.apollographql.apollo3.build"

golatac.init(file("../gradle/libraries.toml"))

dependencies {
  compileOnly(gradleApi())
  compileOnly(golatac.lib("gegp"))

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
  implementation(golatac.lib("intellij.plugin"))
  implementation(golatac.lib("intellij.changelog"))

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

// Keep in sync with CompilerOptions.kt
java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
tasks.withType<JavaCompile>().configureEach {
  options.release.set(11)
}
tasks.withType(KotlinJvmCompile::class.java).configureEach {
  kotlinOptions.jvmTarget = "11"
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
