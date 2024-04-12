plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.compose",
    androidOptions = AndroidOptions(withCompose = true)
)

dependencies {
  api(libs.compose.runtime)
  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}
