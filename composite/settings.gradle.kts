rootProject.name = "apollo-composite"


// Integration Tests
include(":apollo-integration")
project(":apollo-integration").projectDir = file("../apollo-integration")

include(":apollo-integration-kotlin")
project(":apollo-integration-kotlin").projectDir = file("../apollo-integration-kotlin")

// Samples
include(":multiplatform")
project(":multiplatform").projectDir = file("../samples/multiplatform")
include(":multiplatform:kmp-lib-sample")
project(":multiplatform:kmp-lib-sample").projectDir = file("../samples/multiplatform/kmp-lib-sample")

if (System.getProperty("idea.sync.active") == null) {
  include(":kotlin-sample")
  project(":kotlin-sample").projectDir = file("../samples/kotlin-sample")
  include(":multiplatform:kmp-android-app")
  project(":multiplatform:kmp-android-app").projectDir = file("../samples/multiplatform/kmp-android-app")
}

includeBuild("../")
