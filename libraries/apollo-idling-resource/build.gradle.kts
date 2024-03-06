plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.idling.resource",
    androidOptions = AndroidOptions(withCompose = false)
)

dependencies {
  implementation(libs.androidx.espresso.idlingresource)
  api(project(":apollo-runtime"))
}
