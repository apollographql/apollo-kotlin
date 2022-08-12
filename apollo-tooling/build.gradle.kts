plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.library")
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
