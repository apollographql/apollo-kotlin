import com.apollographql.apollo.gradle.api.ApolloExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm.min)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    // Required for Kotlin < 1.6.10
    // See https://kotlinlang.org/docs/whatsnew1620.html#compatibility-changes-in-the-xjvm-default-modes
    freeCompilerArgs += "-Xjvm-default=all"
  }
}
