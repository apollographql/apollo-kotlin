plugins {
  id("com.android.library")
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
