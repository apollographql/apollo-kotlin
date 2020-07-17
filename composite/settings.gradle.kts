rootProject.name = "apollo-composite"


// Integration Tests
include(":apollo-integration")
project(":apollo-integration").projectDir = file("../apollo-integration")

include(":apollo-integration-kotlin")
project(":apollo-integration-kotlin").projectDir = file("../apollo-integration-kotlin")

val skipAndroidModules = extra.properties.get("apollographql_skipAndroidModules") == "true"

if (!skipAndroidModules) {
// Samples
  include(":kotlin-sample")
  project(":kotlin-sample").projectDir = file("../samples/kotlin-sample")
  include(":multiplatform")
  project(":multiplatform").projectDir = file("../samples/multiplatform")
  include(":multiplatform:kmp-android-app")
  project(":multiplatform:kmp-android-app").projectDir = file("../samples/multiplatform/kmp-android-app")
  include(":multiplatform:kmp-lib-sample")
  project(":multiplatform:kmp-lib-sample").projectDir = file("../samples/multiplatform/kmp-lib-sample")
}

includeBuild("../")
