import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(projects.apolloAnnotations)
  implementation(projects.apolloAst)
  api(projects.apolloCompiler)
  implementation(libs.moshi)
  implementation(libs.moshix.sealed.runtime)
  implementation(libs.okhttp)

  implementation(libs.moshi)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
