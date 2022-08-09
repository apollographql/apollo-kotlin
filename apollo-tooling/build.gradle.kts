plugins {
  id("apollo.library.jvm")
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
