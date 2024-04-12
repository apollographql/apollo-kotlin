plugins {
  id("com.android.library")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.compose.paging",
    androidOptions = AndroidOptions(
        withCompose = true
    )
)

dependencies {
  api(project(":apollo-compose-support-incubating"))
  api(libs.androidx.paging.compose)
}
