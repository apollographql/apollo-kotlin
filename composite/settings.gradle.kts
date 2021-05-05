rootProject.name = "composite"

// Integration Tests
include(":integration-tests")
include(":integration-tests-kotlin")

include(":sample-server")
include(":websockets")

// Samples
include(":multiplatform")
project(":multiplatform").projectDir = file("samples/multiplatform")
include(":multiplatform:kmp-lib-sample")
project(":multiplatform:kmp-lib-sample").projectDir = file("samples/multiplatform/kmp-lib-sample")
if (System.getProperty("idea.sync.active") == null) {
    include(":java-sample")
    project(":java-sample").projectDir = file("samples/java-sample")
    include(":kotlin-sample")
    project(":kotlin-sample").projectDir = file("samples/kotlin-sample")
    include(":multiplatform:kmp-android-app")
    project(":multiplatform:kmp-android-app").projectDir = file("samples/multiplatform/kmp-android-app")
}
includeBuild("../build-logic")
includeBuild("../")

